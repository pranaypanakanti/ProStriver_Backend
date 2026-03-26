package com.ProStriver.challenge.dto;

import com.ProStriver.entity.enums.ChallengeType;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class SelectChallengeRequest {
    @NotNull
    private ChallengeType challengeType;
}