package com.proStriver.studyplan;

import com.proStriver.security.ProStriverUserDetails;
import com.springAi.kafka.StudyPlanRequest;
import com.springAi.ratelimit.RateLimitResult;
import com.springAi.ratelimit.RateLimitService;
import com.springAi.studyPlanner.job.StudyPlanJobResponse;
import com.springAi.studyPlanner.job.StudyPlanJobService;
import com.springAi.studyPlanner.job.StudyPlanProgressResponse;
import com.springAi.studyPlanner.job.SubtopicUpdateResult;
import jakarta.validation.Valid;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@Profile("api")
@RequestMapping("/api/study-plan")
public class StudyPlanController {

    private final StudyPlanJobService jobService;
    private final RateLimitService rateLimitService;

    public StudyPlanController(StudyPlanJobService jobService, RateLimitService rateLimitService) {
        this.jobService = jobService;
        this.rateLimitService = rateLimitService;
    }

    @PostMapping
    public ResponseEntity<Map<String, String>> submit(
            @AuthenticationPrincipal ProStriverUserDetails user,
            @RequestBody StudyPlanRequest input) {

        String userId = user.getUserId().toString();

        RateLimitResult rl = rateLimitService.tryConsume(userId);
        if (!rl.allowed()) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .header("Retry-After", String.valueOf(rl.retryAfterSeconds()))
                    .body(Map.of("error", "Rate limit exceeded",
                            "retryAfterSeconds", String.valueOf(rl.retryAfterSeconds())));
        }

        String jobId = jobService.submit(userId, input);
        return ResponseEntity.accepted()
                .header("X-Rate-Limit-Remaining", String.valueOf(rl.remainingTokens()))
                .body(Map.of("jobId", jobId));
    }

    @GetMapping("/{jobId}")
    public ResponseEntity<StudyPlanJobResponse> getJob(
            @PathVariable String jobId,
            @AuthenticationPrincipal ProStriverUserDetails user) {

        String userId = user.getUserId().toString();
        return jobService.findJob(jobId)
                .filter(job -> userId.equals(job.getUserId()))
                .map(StudyPlanJobResponse::from)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PatchMapping("/{jobId}/start")
    public ResponseEntity<StudyPlanProgressResponse> start(
            @PathVariable String jobId,
            @AuthenticationPrincipal ProStriverUserDetails user) {

        String userId = user.getUserId().toString();
        return jobService.startPreparation(jobId, userId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PatchMapping("/{jobId}/subtopic/{subtopicId}")
    public ResponseEntity<StudyPlanProgressResponse> setSubtopicStatus(
            @PathVariable String jobId,
            @PathVariable String subtopicId,
            @AuthenticationPrincipal ProStriverUserDetails user,
            @Valid @RequestBody SubtopicStatusRequest req) {

        String userId = user.getUserId().toString();
        SubtopicUpdateResult result =
                jobService.setSubtopicDone(jobId, userId, subtopicId, req.done());

        return switch (result.outcome()) {
            case UPDATED   -> ResponseEntity.ok(result.progress());
            case NOT_READY -> ResponseEntity.status(HttpStatus.CONFLICT).build();
            case NOT_FOUND -> ResponseEntity.notFound().build();
        };
    }
}