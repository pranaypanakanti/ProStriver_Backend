package com.ProStriver.topic.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class PatchTopicRequest {

    @Size(min = 2, max = 80)
    private String subject;

    @Size(min = 2, max = 160)
    private String title;

    @Size(max = 5000)
    private String notes;
}