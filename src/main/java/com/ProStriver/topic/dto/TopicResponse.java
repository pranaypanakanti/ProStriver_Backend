package com.ProStriver.topic.dto;

import com.ProStriver.entity.enums.TopicStatus;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@AllArgsConstructor
public class TopicResponse {
    private UUID id;
    private String subject;
    private String title;
    private String notes;
    private TopicStatus status;
    private UUID revisionPlanId;
    private String manualReminderPattern;
    private LocalDateTime createdAt;
}