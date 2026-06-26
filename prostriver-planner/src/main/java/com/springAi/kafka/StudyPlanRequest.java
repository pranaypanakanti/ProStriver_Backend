package com.springAi.kafka;

public class StudyPlanRequest {

    private String topic;
    private String timeAvailable;
    private String purpose;
    private String level;
    private String notes;

    public StudyPlanRequest() {}

    public StudyPlanRequest(String topic, String timeAvailable, String purpose,
                            String level, String notes) {
        this.topic = topic;
        this.timeAvailable = timeAvailable;
        this.purpose = purpose;
        this.level = level;
        this.notes = notes;
    }

    public String getTopic() { return topic; }
    public void setTopic(String topic) { this.topic = topic; }

    public String getTimeAvailable() { return timeAvailable; }
    public void setTimeAvailable(String timeAvailable) { this.timeAvailable = timeAvailable; }

    public String getPurpose() { return purpose; }
    public void setPurpose(String purpose) { this.purpose = purpose; }

    public String getLevel() { return level; }
    public void setLevel(String level) { this.level = level; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    @Override
    public String toString() {
        return "StudyPlanRequest{topic='" + topic + "', timeAvailable='" + timeAvailable +
                "', level='" + level + "'}";
    }
}