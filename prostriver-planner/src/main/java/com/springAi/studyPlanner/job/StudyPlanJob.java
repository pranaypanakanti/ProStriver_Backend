package com.springAi.studyPlanner.job;

import com.springAi.kafka.StudyPlanRequest;
import com.springAi.studyPlanner.entities.StudyPlanResponse;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document(collection = "study_plans")
public class StudyPlanJob {

    @Id
    private String id;

    @Indexed(unique = true)
    private String jobId;

    private String userId;

    private Tier tier;
    private JobStatus status;

    private StudyPlanRequest input;
    private boolean startPreparation;

    private StudyPlanResponse plan;


    private int totalSubtopics;
    private int completedSubtopics;

    private Instant createdAt;
    private Instant updatedAt;

    public int getCompletedSubtopics() { return completedSubtopics; }

    public void setCompletedSubtopics(int completedSubtopics) { this.completedSubtopics = completedSubtopics; }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getJobId() {
        return jobId;
    }

    public void setJobId(String jobId) {
        this.jobId = jobId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public Tier getTier() {
        return tier;
    }

    public void setTier(Tier tier) {
        this.tier = tier;
    }

    public JobStatus getStatus() {
        return status;
    }

    public void setStatus(JobStatus status) {
        this.status = status;
    }

    public StudyPlanRequest getInput() {
        return input;
    }

    public void setInput(StudyPlanRequest input) {
        this.input = input;
    }

    public boolean isStartPreparation() {
        return startPreparation;
    }

    public void setStartPreparation(boolean startPreparation) {
        this.startPreparation = startPreparation;
    }

    public StudyPlanResponse getPlan() {
        return plan;
    }

    public void setPlan(StudyPlanResponse plan) {
        this.plan = plan;
    }

    public int getTotalSubtopics() {
        return totalSubtopics;
    }

    public void setTotalSubtopics(int totalSubtopics) {
        this.totalSubtopics = totalSubtopics;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}