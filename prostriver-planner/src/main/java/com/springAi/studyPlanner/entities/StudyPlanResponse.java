package com.springAi.studyPlanner.entities;

import java.util.List;

public class StudyPlanResponse {

    private GoalOverview goalOverview;
    private List<MainTopic> mainTopics;
    private List<String> nextTopics;
    private List<String> opportunities;
    private List<String> quickRevision;

    public GoalOverview getGoalOverview() {
        return goalOverview;
    }

    public void setGoalOverview(GoalOverview goalOverview) {
        this.goalOverview = goalOverview;
    }

    public List<MainTopic> getMainTopics() {
        return mainTopics;
    }

    public void setMainTopics(List<MainTopic> mainTopics) {
        this.mainTopics = mainTopics;
    }

    public List<String> getNextTopics() {
        return nextTopics;
    }

    public void setNextTopics(List<String> nextTopics) {
        this.nextTopics = nextTopics;
    }

    public List<String> getOpportunities() {
        return opportunities;
    }

    public void setOpportunities(List<String> opportunities) {
        this.opportunities = opportunities;
    }

    public List<String> getQuickRevision() {
        return quickRevision;
    }

    public void setQuickRevision(List<String> quickRevision) {
        this.quickRevision = quickRevision;
    }
}