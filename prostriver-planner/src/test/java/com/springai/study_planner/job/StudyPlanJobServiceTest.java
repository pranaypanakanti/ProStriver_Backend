package com.springai.study_planner.job;

import com.springai.MongoIntegrationBase;
import com.springai.kafka.StudyPlanProducer;
import com.springai.study_planner.entities.MainTopic;
import com.springai.study_planner.entities.StudyPlanResponse;
import com.springai.study_planner.entities.SubTopic;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link StudyPlanJobService#setSubtopicDone}.
 *
 * <p>These tests MUST run against a real MongoDB container because the
 * implementation uses {@code $[]}/arrayFilter in a {@code findAndModify}
 * operation — behaviour that a mock cannot replicate or verify.
 *
 * <p>Context setup:
 * <ul>
 *   <li>{@code @DataMongoTest} enables only Mongo-related auto-configuration
 *       and scans for Mongo repositories. All other auto-configuration
 *       (Redis, Kafka, etc.) is disabled.</li>
 *   <li>{@code @Import(StudyPlanJobService.class)} adds the service under
 *       test, which is not picked up by the Mongo slice filter on its own.</li>
 *   <li>{@code @MockitoBean StudyPlanProducer} satisfies the service's Kafka
 *       dependency without starting a broker.</li>
 * </ul>
 */
@DataMongoTest
@Import(StudyPlanJobService.class)
class StudyPlanJobServiceTest extends MongoIntegrationBase {

    // ---- collaborators --------------------------------------------------

    @MockitoBean
    @SuppressWarnings("unused")     // injected into StudyPlanJobService by Spring
    private StudyPlanProducer producer;

    @Autowired
    private StudyPlanJobService service;

    @Autowired
    private StudyPlanJobRepository repository;

    // ---- fixtures -------------------------------------------------------

    private static final String USER_ID        = "user-aaa";
    private static final String OTHER_USER_ID  = "user-bbb";
    private static final String SUBTOPIC_ID    = "sub-001";
    private static final String UNKNOWN_ID     = "sub-NONEXISTENT";

    @BeforeEach
    void cleanDatabase() {
        repository.deleteAll();
    }

    // ---- tests ----------------------------------------------------------

    /**
     * Happy path: marking a subtopic done sets {@code done=true} and
     * increments {@code completedSubtopics} by 1.
     */
    @Test
    void setSubtopicDone_markDone_setsDoneTrueAndIncrementsCount() {
        StudyPlanJob job = persistJob(USER_ID, JobStatus.DONE, /*subtopicDone=*/ false, /*completedCount=*/ 0);

        SubtopicUpdateResult result = service.setSubtopicDone(job.getJobId(), USER_ID, SUBTOPIC_ID, true);

        assertThat(result.outcome()).isEqualTo(SubtopicUpdateResult.Outcome.UPDATED);
        assertThat(result.progress().getCompletedSubtopics()).isEqualTo(1);

        StudyPlanJob reloaded = repository.findByJobId(job.getJobId()).orElseThrow();
        assertThat(reloaded.getCompletedSubtopics()).isEqualTo(1);
        assertThat(subtopicIsDone(reloaded, SUBTOPIC_ID)).isTrue();
    }

    /**
     * Marking a subtopic undone sets {@code done=false} and decrements
     * {@code completedSubtopics} by 1.
     */
    @Test
    void setSubtopicDone_markUndone_setsDoneFalseAndDecrementsCount() {
        StudyPlanJob job = persistJob(USER_ID, JobStatus.DONE, /*subtopicDone=*/ true, /*completedCount=*/ 1);

        SubtopicUpdateResult result = service.setSubtopicDone(job.getJobId(), USER_ID, SUBTOPIC_ID, false);

        assertThat(result.outcome()).isEqualTo(SubtopicUpdateResult.Outcome.UPDATED);
        assertThat(result.progress().getCompletedSubtopics()).isEqualTo(0);

        StudyPlanJob reloaded = repository.findByJobId(job.getJobId()).orElseThrow();
        assertThat(reloaded.getCompletedSubtopics()).isEqualTo(0);
        assertThat(subtopicIsDone(reloaded, SUBTOPIC_ID)).isFalse();
    }

    /**
     * Idempotency invariant: clicking "mark done" a second time when the
     * subtopic is already done must NOT increment the counter.
     *
     * <p>Mechanism: the gate query in {@code findAndModify} requires
     * {@code done = !targetDone}, so it matches nothing when the subtopic is
     * already in the target state. The service then returns the current
     * document via the repository fallback without issuing any {@code $inc}.
     */
    @Test
    void setSubtopicDone_alreadyDone_countUnchanged() {
        StudyPlanJob job = persistJob(USER_ID, JobStatus.DONE, /*subtopicDone=*/ true, /*completedCount=*/ 1);

        SubtopicUpdateResult result = service.setSubtopicDone(job.getJobId(), USER_ID, SUBTOPIC_ID, true);

        assertThat(result.outcome()).isEqualTo(SubtopicUpdateResult.Outcome.UPDATED);
        // The fallback path returns the stored document; counter must still be 1
        assertThat(result.progress().getCompletedSubtopics()).isEqualTo(1);

        // Reload from Mongo to confirm no drift occurred in the database
        StudyPlanJob reloaded = repository.findByJobId(job.getJobId()).orElseThrow();
        assertThat(reloaded.getCompletedSubtopics()).isEqualTo(1);
        // The subtopic must still be marked done
        assertThat(subtopicIsDone(reloaded, SUBTOPIC_ID)).isTrue();
    }

    /**
     * When the requested subtopicId does not exist in the plan, the service
     * must return NOT_FOUND.
     */
    @Test
    void setSubtopicDone_unknownSubtopicId_returnsNotFound() {
        StudyPlanJob job = persistJob(USER_ID, JobStatus.DONE, /*subtopicDone=*/ false, /*completedCount=*/ 0);

        SubtopicUpdateResult result = service.setSubtopicDone(job.getJobId(), USER_ID, UNKNOWN_ID, true);

        assertThat(result.outcome()).isEqualTo(SubtopicUpdateResult.Outcome.NOT_FOUND);
    }

    /**
     * When the caller's userId does not match the job's owner, the service
     * must return NOT_FOUND (never NOT_FOUND for the subtopic and never 403 —
     * ownership errors must be indistinguishable from missing resources).
     */
    @Test
    void setSubtopicDone_wrongUserId_returnsNotFound() {
        StudyPlanJob job = persistJob(USER_ID, JobStatus.DONE, /*subtopicDone=*/ false, /*completedCount=*/ 0);

        SubtopicUpdateResult result = service.setSubtopicDone(job.getJobId(), OTHER_USER_ID, SUBTOPIC_ID, true);

        assertThat(result.outcome()).isEqualTo(SubtopicUpdateResult.Outcome.NOT_FOUND);
    }

    /**
     * When the job status is not DONE (plan not yet generated), the service
     * must return NOT_READY.
     *
     * <p>A job with status QUEUED has no plan document, so the gate query
     * finds nothing. The repository fallback finds the job (correct userId)
     * but the status check triggers the NOT_READY branch.
     */
    @Test
    void setSubtopicDone_planNotDone_returnsNotReady() {
        StudyPlanJob job = persistJobWithoutPlan(USER_ID, JobStatus.QUEUED);

        SubtopicUpdateResult result = service.setSubtopicDone(job.getJobId(), USER_ID, SUBTOPIC_ID, true);

        assertThat(result.outcome()).isEqualTo(SubtopicUpdateResult.Outcome.NOT_READY);
    }

    // ---- helpers --------------------------------------------------------

    /**
     * Persists a job that has a plan containing one main topic with one
     * subtopic whose {@code done} flag and the job's {@code completedSubtopics}
     * counter are set to the supplied values.
     */
    private StudyPlanJob persistJob(String userId, JobStatus status,
                                    boolean subtopicDone, int completedCount) {
        SubTopic subTopic = new SubTopic();
        subTopic.setSubtopicId(SUBTOPIC_ID);
        subTopic.setSubTopicName("Fixture subtopic");
        subTopic.setDone(subtopicDone);

        MainTopic mainTopic = new MainTopic();
        mainTopic.setTopicId("topic-1");
        mainTopic.setTopicName("Fixture topic");
        mainTopic.setSubTopics(List.of(subTopic));

        StudyPlanResponse plan = new StudyPlanResponse();
        plan.setMainTopics(List.of(mainTopic));

        return repository.save(buildJob(userId, status, plan, 1, completedCount));
    }

    /**
     * Persists a job that has NO plan (e.g., still QUEUED or PROCESSING).
     */
    private StudyPlanJob persistJobWithoutPlan(String userId, JobStatus status) {
        return repository.save(buildJob(userId, status, null, 0, 0));
    }

    private StudyPlanJob buildJob(String userId, JobStatus status,
                                  StudyPlanResponse plan,
                                  int totalSubtopics, int completedSubtopics) {
        StudyPlanJob job = new StudyPlanJob();
        job.setJobId(UUID.randomUUID().toString());
        job.setUserId(userId);
        job.setStatus(status);
        job.setPlan(plan);
        job.setTotalSubtopics(totalSubtopics);
        job.setCompletedSubtopics(completedSubtopics);
        job.setTier(Tier.FREE);
        job.setStartPreparation(false);
        Instant now = Instant.now();
        job.setCreatedAt(now);
        job.setUpdatedAt(now);
        return job;
    }

    /**
     * Returns the {@code done} flag for the named subtopic in the given job.
     * Throws {@link AssertionError} if the subtopic is not found (guards
     * against a mis-wired fixture silently passing the test).
     */
    private boolean subtopicIsDone(StudyPlanJob job, String subtopicId) {
        return job.getPlan().getMainTopics().stream()
                .flatMap(mt -> mt.getSubTopics().stream())
                .filter(st -> subtopicId.equals(st.getSubtopicId()))
                .findFirst()
                .map(SubTopic::isDone)
                .orElseThrow(() -> new AssertionError(
                        "Subtopic '" + subtopicId + "' not found in reloaded document"));
    }
}
