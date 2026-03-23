package com.kronos.topic.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.UUID;

@Data
public class CreateTopicRequest {

    @NotBlank
    @Size(min = 2, max = 80)
    private String subject;

    @NotBlank
    @Size(min = 2, max = 160)
    private String title;

    @Size(max = 5000)
    private String notes;

    private UUID revisionPlanId;

    private String manualReminderPattern;
}