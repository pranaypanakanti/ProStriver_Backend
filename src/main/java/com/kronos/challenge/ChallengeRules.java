package com.kronos.challenge;

import com.kronos.entity.enums.ChallengeType;

public final class ChallengeRules {

    private ChallengeRules() {}

    public static int durationDays(ChallengeType type) {
        return switch (type) {
            case THIRTY_DAY -> 30;
            case HUNDRED_DAY -> 100;
            case TWO_HUNDRED_DAY -> 200;
            case THREE_SIXTY_FIVE_DAY -> 365;
        };
    }

    public static int freezeAllowed(ChallengeType type) {
        int duration = durationDays(type);
        return (int) Math.floor(duration * 0.10);
    }
}