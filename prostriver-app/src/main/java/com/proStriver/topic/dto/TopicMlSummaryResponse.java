package com.proStriver.topic.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class TopicMlSummaryResponse {
    private String subject;
    private String title;
    private LocalDateTime createdAt;
}