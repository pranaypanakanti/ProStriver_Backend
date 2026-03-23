package com.kronos.analytics.dto;

import com.kronos.entity.enums.ChallengeStatus;
import com.kronos.entity.enums.ChallengeType;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDate;
import java.util.UUID;

@Data
@AllArgsConstructor
public class AnalyticsOverviewResponse {

    private RevisionGauge revision;
    private ChallengeInfo challenge; // nullable

    @Data
    @AllArgsConstructor
    public static class RevisionGauge {
        private int completedMtd;
        private int missedMtd;
        private int emailedMtd;
        private double rateMtd; // 0..1
        private int month;
        private int year;
    }

    @Data
    @AllArgsConstructor
    public static class ChallengeInfo {
        private UUID id;
        private ChallengeType type;
        private ChallengeStatus status;
        private int durationDays;
        private LocalDate startDate;
        private LocalDate endDate;
        private LocalDate evaluatedThroughDate;
        private int qualifiedDays;
        private int currentStreak;
        private int freezeAllowed;
        private int freezeUsed;
        private int freezeRemaining;
        private double progressPercent;   // 0..100 (currentStreak / durationDays)
        private double completionRate;    // 0..1 (qualifiedDays / durationDays)
    }
}