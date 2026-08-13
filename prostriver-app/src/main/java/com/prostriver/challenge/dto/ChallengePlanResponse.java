package com.prostriver.challenge.dto;

import com.prostriver.entity.enums.ChallengeType;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ChallengePlanResponse {
    private ChallengeType type;
    private int durationDays;
    private int freezeAllowed;
}