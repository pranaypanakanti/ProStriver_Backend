package com.ProStriver.admin.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateRevisionPlanRequest {

    @Size(min = 1, max = 40)
    private String name;

    @Size(max = 255)
    private String description;

    @Pattern(regexp = "^[0-9]+(\\s*,\\s*[0-9]+)*$", message = "revisionDaysPattern must look like '1,4,7'")
    private String revisionDaysPattern;
}