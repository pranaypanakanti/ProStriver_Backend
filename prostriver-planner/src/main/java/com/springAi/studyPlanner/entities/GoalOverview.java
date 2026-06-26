package com.springAi.studyPlanner.entities;

public class GoalOverview {

    private String topic;
    private String purpose;
    private String currentLevel;
    private String totalTimeAvailable;
    private String recommendedDailyStudyTime;
    private String expectedOutcome;

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public String getPurpose() {
        return purpose;
    }

    public void setPurpose(String purpose) {
        this.purpose = purpose;
    }

    public String getCurrentLevel() {
        return currentLevel;
    }

    public void setCurrentLevel(String currentLevel) {
        this.currentLevel = currentLevel;
    }

    public String getTotalTimeAvailable() {
        return totalTimeAvailable;
    }

    public void setTotalTimeAvailable(String totalTimeAvailable) {
        this.totalTimeAvailable = totalTimeAvailable;
    }

    public String getRecommendedDailyStudyTime() {
        return recommendedDailyStudyTime;
    }

    public void setRecommendedDailyStudyTime(String recommendedDailyStudyTime) {
        this.recommendedDailyStudyTime = recommendedDailyStudyTime;
    }

    public String getExpectedOutcome() {
        return expectedOutcome;
    }

    public void setExpectedOutcome(String expectedOutcome) {
        this.expectedOutcome = expectedOutcome;
    }
}