package com.ProStriver.topic.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDate;
import java.util.UUID;

@Data
@AllArgsConstructor
public class TodayRevisionItemResponse {
    private UUID revisionScheduleId;
    private UUID topicId;
    private String subject;
    private String title;
    private int dayNumber;
    private LocalDate scheduledDate;
}