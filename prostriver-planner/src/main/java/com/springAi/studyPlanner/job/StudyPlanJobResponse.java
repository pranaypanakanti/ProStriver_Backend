package com.springAi.studyPlanner.job;

import com.springAi.studyPlanner.entities.StudyPlanResponse;

public class StudyPlanJobResponse {

    private String jobId;
    private JobStatus status;
    private boolean startPreparation;
    private int totalSubtopics;
    private int completedSubtopics;
    private StudyPlanResponse plan;

    public static StudyPlanJobResponse from(StudyPlanJob job) {
        StudyPlanJobResponse r = new StudyPlanJobResponse();
        r.jobId = job.getJobId();
        r.status = job.getStatus();
        r.startPreparation = job.isStartPreparation();
        r.totalSubtopics = job.getTotalSubtopics();
        r.completedSubtopics = job.getCompletedSubtopics();
        r.plan = job.getPlan();
        return r;
    }

    public String getJobId() {
        return jobId;
    }

    public JobStatus getStatus() {
        return status;
    }

    public boolean isStartPreparation() {
        return startPreparation;
    }

    public int getTotalSubtopics() {
        return totalSubtopics;
    }

    public int getCompletedSubtopics() { return completedSubtopics; }

    public StudyPlanResponse getPlan() {
        return plan;
    }
}