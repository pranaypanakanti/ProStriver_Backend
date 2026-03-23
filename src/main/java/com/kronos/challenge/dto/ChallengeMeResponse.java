package com.kronos.challenge.dto;

import com.kronos.entity.enums.ChallengeStatus;
import com.kronos.entity.enums.ChallengeType;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDate;
import java.util.UUID;

@Data
@AllArgsConstructor
public class ChallengeMeResponse {
    private UUID id;
    private ChallengeType challengeType;
    private ChallengeStatus status;
    private int durationDays;
    private LocalDate startDate;
    private LocalDate endDate;
    private int currentStreak;
    private int freezeAllowed;
    private int freezeUsed;
    private int freezeRemaining;
    private double progressPercent;   // 0..100
    private double completionRate;    // 0..1 (days qualified / durationDays)
}