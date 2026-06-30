package com.springAi.studyPlanner.entities;

import java.util.List;

public class SubTopic {

    private String subtopicId;
    private String subTopicName;
    private String whatToLearn;
    private List<String> keyConcepts;
    private boolean done;

    public String getSubtopicId() {
        return subtopicId;
    }

    public void setSubtopicId(String subtopicId) {
        this.subtopicId = subtopicId;
    }

    public String getSubTopicName() {
        return subTopicName;
    }

    public void setSubTopicName(String subTopicName) {
        this.subTopicName = subTopicName;
    }

    public String getWhatToLearn() {
        return whatToLearn;
    }

    public void setWhatToLearn(String whatToLearn) {
        this.whatToLearn = whatToLearn;
    }

    public List<String> getKeyConcepts() {
        return keyConcepts;
    }

    public void setKeyConcepts(List<String> keyConcepts) {
        this.keyConcepts = keyConcepts;
    }

    public boolean isDone() {
        return done;
    }

    public void setDone(boolean done) {
        this.done = done;
    }
}