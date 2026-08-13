package com.springAi.studyPlanner.job;

import com.springAi.kafka.StudyPlanRequest;

import java.time.Instant;

public record StudyPlanSummaryResponse(
        String jobId,
        JobStatus status,
        Tier tier,
        StudyPlanRequest input,
        boolean startPreparation,
        int totalSubtopics,
        int completedSubtopics,
        Instant createdAt,
        Instant updatedAt
) {
    public static StudyPlanSummaryResponse from(StudyPlanJob job) {
        return new StudyPlanSummaryResponse(
                job.getJobId(),
                job.getStatus(),
                job.getTier(),
                job.getInput(),
                job.isStartPreparation(),
                job.getTotalSubtopics(),
                job.getCompletedSubtopics(),
                job.getCreatedAt(),
                job.getUpdatedAt()
        );
    }
}