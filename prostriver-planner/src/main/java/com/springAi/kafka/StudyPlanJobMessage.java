package com.springAi.kafka;

public class StudyPlanJobMessage {

    private String jobId;
    private String userId;

    public StudyPlanJobMessage() {}

    public StudyPlanJobMessage(String jobId, String userId) {
        this.jobId = jobId;
        this.userId = userId;
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
}