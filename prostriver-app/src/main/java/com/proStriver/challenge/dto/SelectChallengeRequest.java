package com.proStriver.challenge.dto;

import com.proStriver.entity.enums.ChallengeType;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class SelectChallengeRequest {
    @NotNull
    private ChallengeType challengeType;
}