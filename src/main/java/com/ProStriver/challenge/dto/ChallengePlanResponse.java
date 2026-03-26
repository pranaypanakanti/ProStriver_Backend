package com.ProStriver.challenge.dto;

import com.ProStriver.entity.enums.ChallengeType;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ChallengePlanResponse {
    private ChallengeType type;
    private int durationDays;
    private int freezeAllowed;
}