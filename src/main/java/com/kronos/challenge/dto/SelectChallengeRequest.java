package com.kronos.challenge.dto;

import com.kronos.entity.enums.ChallengeType;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class SelectChallengeRequest {
    @NotNull
    private ChallengeType challengeType;
}