package com.ProStriver.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.UUID;

@Data
@AllArgsConstructor
public class AdminRevisionPlanResponse {
    private UUID id;
    private String name;
    private String description;
    private String revisionDaysPattern;
}