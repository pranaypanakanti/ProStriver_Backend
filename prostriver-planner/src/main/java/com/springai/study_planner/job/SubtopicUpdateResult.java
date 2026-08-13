package com.springai.study_planner.job;

public record SubtopicUpdateResult(Outcome outcome, StudyPlanProgressResponse progress) {

    public enum Outcome { UPDATED, NOT_FOUND, NOT_READY }

    public static SubtopicUpdateResult updated(StudyPlanProgressResponse progress) {
        return new SubtopicUpdateResult(Outcome.UPDATED, progress);
    }
    public static SubtopicUpdateResult notFound() {
        return new SubtopicUpdateResult(Outcome.NOT_FOUND, null);
    }
    public static SubtopicUpdateResult notReady() {
        return new SubtopicUpdateResult(Outcome.NOT_READY, null);
    }
}