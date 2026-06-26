package com.springAi.studyPlanner.entities;

import java.util.List;

public class MainTopic {

    private String topicId;
    private String topicName;
    private String estimatedStudyTime;
    private String difficultyLevel;
    private String importanceLevel;
    private String completionOutcome;

    private List<SubTopic> subTopics;
    private ResourceSection resources;

    private List<String> commonMistakes;
    private List<String> keyTips;

    public String getTopicId() {
        return topicId;
    }

    public void setTopicId(String topicId) {
        this.topicId = topicId;
    }

    public String getTopicName() {
        return topicName;
    }

    public void setTopicName(String topicName) {
        this.topicName = topicName;
    }

    public String getEstimatedStudyTime() {
        return estimatedStudyTime;
    }

    public void setEstimatedStudyTime(String estimatedStudyTime) {
        this.estimatedStudyTime = estimatedStudyTime;
    }

    public String getDifficultyLevel() {
        return difficultyLevel;
    }

    public void setDifficultyLevel(String difficultyLevel) {
        this.difficultyLevel = difficultyLevel;
    }

    public String getImportanceLevel() {
        return importanceLevel;
    }

    public void setImportanceLevel(String importanceLevel) {
        this.importanceLevel = importanceLevel;
    }

    public String getCompletionOutcome() {
        return completionOutcome;
    }

    public void setCompletionOutcome(String completionOutcome) {
        this.completionOutcome = completionOutcome;
    }

    public List<SubTopic> getSubTopics() {
        return subTopics;
    }

    public void setSubTopics(List<SubTopic> subTopics) {
        this.subTopics = subTopics;
    }

    public ResourceSection getResources() {
        return resources;
    }

    public void setResources(ResourceSection resources) {
        this.resources = resources;
    }

    public List<String> getCommonMistakes() {
        return commonMistakes;
    }

    public void setCommonMistakes(List<String> commonMistakes) {
        this.commonMistakes = commonMistakes;
    }

    public List<String> getKeyTips() {
        return keyTips;
    }

    public void setKeyTips(List<String> keyTips) {
        this.keyTips = keyTips;
    }
}