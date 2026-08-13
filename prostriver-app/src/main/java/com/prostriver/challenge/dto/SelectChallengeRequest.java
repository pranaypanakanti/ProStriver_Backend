package com.prostriver.challenge.dto;

import com.prostriver.entity.enums.ChallengeType;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class SelectChallengeRequest {
    @NotNull
    private ChallengeType challengeType;
}