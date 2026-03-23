package com.kronos.challenge.dto;

import com.kronos.entity.enums.ChallengeType;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ChallengePlanResponse {
    private ChallengeType type;
    private int durationDays;
    private int freezeAllowed;
}