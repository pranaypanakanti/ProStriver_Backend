package com.prostriver.studyplan;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.prostriver.entity.User;
import com.prostriver.entity.enums.Role;
import com.prostriver.security.JwtService;
import com.prostriver.security.ProStriverUserDetails;
import com.prostriver.security.ProStriverUserDetailsService;
import com.springai.kafka.StudyPlanRequest;
import com.springai.ratelimit.RateLimitResult;
import com.springai.ratelimit.RateLimitService;
import com.springai.study_planner.job.JobStatus;
import com.springai.study_planner.job.StudyPlanJob;
import com.springai.study_planner.job.StudyPlanJobService;
import com.springai.study_planner.job.StudyPlanProgressResponse;
import com.springai.study_planner.job.SubtopicUpdateResult;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * {@code @WebMvcTest} slice for {@link StudyPlanController}.
 *
 * <p>Verifies HTTP status/header/body mapping and the ownership-hides-as-404
 * pattern. {@link StudyPlanJobService} and {@link RateLimitService} are
 * mocked. The real {@code SecurityConfig} is NOT imported, but
 * {@code @WebMvcTest} auto-scans {@code Filter}-typed {@code @Component}
 * beans regardless of whether {@code SecurityConfig} is imported, so
 * {@code JwtAuthenticationFilter} (which is {@code @Profile("api")} and
 * constructor-injects {@link JwtService} and
 * {@link ProStriverUserDetailsService}) is still picked up and needs those
 * two collaborators mocked purely to satisfy the bean graph. Neither mock is
 * ever stubbed: none of the requests below send an {@code Authorization:
 * Bearer ...} header, so {@code JwtAuthenticationFilter.doFilterInternal}
 * takes its early-return branch and never touches them. Authentication for
 * these tests is established solely via
 * {@code SecurityMockMvcRequestPostProcessors.user(...)}, and Spring
 * Security's default web-mvc-test auto-configuration is what requires
 * authentication on every endpoint and returns 401 when it is missing.
 */
@WebMvcTest(StudyPlanController.class)
class StudyPlanControllerTest {

    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID OTHER_USER_ID = UUID.randomUUID();
    private static final String JOB_ID = "job-123";
    private static final String SUBTOPIC_ID = "sub-001";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private StudyPlanJobService jobService;

    @MockitoBean
    private RateLimitService rateLimitService;

    // Not exercised directly: JwtAuthenticationFilter is picked up by
    // @WebMvcTest as a Filter-typed bean regardless of whether SecurityConfig
    // is imported, so these two collaborators must be mocked to satisfy its
    // constructor. Every request in this class omits the Authorization
    // header, so the filter's early-return branch means neither mock is ever
    // invoked.
    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private ProStriverUserDetailsService userDetailsService;

    // ---- fixtures ---------------------------------------------------------

    private ProStriverUserDetails principal(UUID userId) {
        User fakeUser = new User();
        fakeUser.setId(userId);
        fakeUser.setRole(Role.USER);
        return new ProStriverUserDetails(fakeUser);
    }

    private StudyPlanJob jobOwnedBy(UUID userId) {
        StudyPlanJob job = new StudyPlanJob();
        job.setJobId(JOB_ID);
        job.setUserId(userId.toString());
        job.setStatus(JobStatus.DONE);
        job.setStartPreparation(true);
        job.setTotalSubtopics(3);
        job.setCompletedSubtopics(1);
        return job;
    }

    private StudyPlanProgressResponse progressFor(StudyPlanJob job) {
        return StudyPlanProgressResponse.from(job);
    }

    // ---- POST / (submit) ---------------------------------------------------

    @Test
    void submit_allowedByRateLimit_returns202WithJobIdAndRemainingHeader() throws Exception {
        when(rateLimitService.tryConsume(USER_ID.toString()))
                .thenReturn(new RateLimitResult(true, 4, 0));
        when(jobService.submit(eq(USER_ID.toString()), any(StudyPlanRequest.class)))
                .thenReturn(JOB_ID);

        StudyPlanRequest input = new StudyPlanRequest("Java", "2 weeks", "interview", "beginner", null);

        mockMvc.perform(post("/api/study-plan")
                        .with(user(principal(USER_ID)))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(input)))
                .andExpect(status().isAccepted())
                .andExpect(header().string("X-Rate-Limit-Remaining", "4"))
                .andExpect(jsonPath("$.jobId").value(JOB_ID));
    }

    @Test
    void submit_rateLimited_returns429AndNeverCallsJobService() throws Exception {
        when(rateLimitService.tryConsume(USER_ID.toString()))
                .thenReturn(new RateLimitResult(false, 0, 120));

        StudyPlanRequest input = new StudyPlanRequest("Java", "2 weeks", "interview", "beginner", null);

        mockMvc.perform(post("/api/study-plan")
                        .with(user(principal(USER_ID)))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(input)))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().string("Retry-After", "120"));

        verify(jobService, never()).submit(anyString(), any(StudyPlanRequest.class));
    }

    @Test
    void submit_malformedJsonBody_returns400() throws Exception {
        mockMvc.perform(post("/api/study-plan")
                        .with(user(principal(USER_ID)))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{not-valid-json"))
                .andExpect(status().isBadRequest());
    }

    // ---- GET /{jobId} (getJob) ---------------------------------------------

    @Test
    void getJob_foundAndOwned_returns200WithJobFields() throws Exception {
        StudyPlanJob job = jobOwnedBy(USER_ID);
        when(jobService.findJob(JOB_ID)).thenReturn(Optional.of(job));

        mockMvc.perform(get("/api/study-plan/{jobId}", JOB_ID)
                        .with(user(principal(USER_ID))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.jobId").value(JOB_ID))
                .andExpect(jsonPath("$.status").value("DONE"))
                .andExpect(jsonPath("$.totalSubtopics").value(3))
                .andExpect(jsonPath("$.completedSubtopics").value(1));
    }

    @Test
    void getJob_notFound_returns404() throws Exception {
        when(jobService.findJob(JOB_ID)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/study-plan/{jobId}", JOB_ID)
                        .with(user(principal(USER_ID))))
                .andExpect(status().isNotFound());
    }

    @Test
    void getJob_ownedByDifferentUser_returns404() throws Exception {
        StudyPlanJob job = jobOwnedBy(OTHER_USER_ID);
        when(jobService.findJob(JOB_ID)).thenReturn(Optional.of(job));

        mockMvc.perform(get("/api/study-plan/{jobId}", JOB_ID)
                        .with(user(principal(USER_ID))))
                .andExpect(status().isNotFound());
    }

    @Test
    void getJob_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/api/study-plan/{jobId}", JOB_ID))
                .andExpect(status().isUnauthorized());
    }

    // ---- PATCH /{jobId}/start (start) --------------------------------------

    @Test
    void start_found_returns200() throws Exception {
        StudyPlanJob job = jobOwnedBy(USER_ID);
        when(jobService.startPreparation(JOB_ID, USER_ID.toString()))
                .thenReturn(Optional.of(progressFor(job)));

        mockMvc.perform(patch("/api/study-plan/{jobId}/start", JOB_ID)
                        .with(user(principal(USER_ID)))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.jobId").value(JOB_ID));
    }

    @Test
    void start_notFound_returns404() throws Exception {
        when(jobService.startPreparation(JOB_ID, USER_ID.toString()))
                .thenReturn(Optional.empty());

        mockMvc.perform(patch("/api/study-plan/{jobId}/start", JOB_ID)
                        .with(user(principal(USER_ID)))
                        .with(csrf()))
                .andExpect(status().isNotFound());
    }

    // No separate "owned by different user" test for /start: unlike getJob,
    // StudyPlanJobService.startPreparation(jobId, userId) filters by userId
    // *inside* the Mongo query itself, so from this controller/@WebMvcTest
    // slice's perspective a wrong-owner call and a genuinely nonexistent job
    // are indistinguishable — both are mocked as Optional.empty() and hit the
    // exact same controller branch already exercised above by
    // start_notFound_returns404. A dedicated test would just re-run the same
    // mock/assertion under a different name, so it's intentionally omitted
    // here; the ownership-scoping behavior of startPreparation itself belongs
    // to StudyPlanJobService's own test suite.

    // ---- PATCH /{jobId}/subtopic/{subtopicId} (setSubtopicStatus) ----------

    @Test
    void setSubtopicStatus_updatedOutcome_returns200WithProgressBody() throws Exception {
        StudyPlanJob job = jobOwnedBy(USER_ID);
        when(jobService.setSubtopicDone(JOB_ID, USER_ID.toString(), SUBTOPIC_ID, true))
                .thenReturn(SubtopicUpdateResult.updated(progressFor(job)));

        mockMvc.perform(patch("/api/study-plan/{jobId}/subtopic/{subtopicId}", JOB_ID, SUBTOPIC_ID)
                        .with(user(principal(USER_ID)))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"done\": true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.jobId").value(JOB_ID))
                .andExpect(jsonPath("$.completedSubtopics").value(1));
    }

    @Test
    void setSubtopicStatus_notReadyOutcome_returns409() throws Exception {
        when(jobService.setSubtopicDone(JOB_ID, USER_ID.toString(), SUBTOPIC_ID, true))
                .thenReturn(SubtopicUpdateResult.notReady());

        mockMvc.perform(patch("/api/study-plan/{jobId}/subtopic/{subtopicId}", JOB_ID, SUBTOPIC_ID)
                        .with(user(principal(USER_ID)))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"done\": true}"))
                .andExpect(status().isConflict());
    }

    @Test
    void setSubtopicStatus_notFoundOutcome_returns404() throws Exception {
        when(jobService.setSubtopicDone(JOB_ID, USER_ID.toString(), SUBTOPIC_ID, true))
                .thenReturn(SubtopicUpdateResult.notFound());

        mockMvc.perform(patch("/api/study-plan/{jobId}/subtopic/{subtopicId}", JOB_ID, SUBTOPIC_ID)
                        .with(user(principal(USER_ID)))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"done\": true}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void setSubtopicStatus_missingDoneField_returns400() throws Exception {
        mockMvc.perform(patch("/api/study-plan/{jobId}/subtopic/{subtopicId}", JOB_ID, SUBTOPIC_ID)
                        .with(user(principal(USER_ID)))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }
}
