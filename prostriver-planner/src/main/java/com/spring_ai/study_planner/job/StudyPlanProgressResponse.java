package com.spring_ai.study_planner.job;

public class StudyPlanProgressResponse {

    private String jobId;
    private JobStatus status;
    private boolean startPreparation;
    private int totalSubtopics;
    private int completedSubtopics;

    public static StudyPlanProgressResponse from(StudyPlanJob job) {
        StudyPlanProgressResponse r = new StudyPlanProgressResponse();
        r.jobId = job.getJobId();
        r.status = job.getStatus();
        r.startPreparation = job.isStartPreparation();
        r.totalSubtopics = job.getTotalSubtopics();
        r.completedSubtopics = job.getCompletedSubtopics();
        return r;
    }

    public String getJobId() { return jobId; }
    public JobStatus getStatus() { return status; }
    public boolean isStartPreparation() { return startPreparation; }
    public int getTotalSubtopics() { return totalSubtopics; }
    public int getCompletedSubtopics() { return completedSubtopics; }
}