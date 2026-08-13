package com.prostriver.challenge;

import com.prostriver.entity.DailyProgress;
import com.prostriver.entity.LockInChallenge;
import com.prostriver.entity.enums.ChallengeStatus;
import com.prostriver.repository.DailyProgressRepository;
import com.prostriver.repository.LockInChallengeRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.UUID;

@Profile("worker")
@Component
@RequiredArgsConstructor
public class ChallengeEvaluator {

    private static final Logger log = LoggerFactory.getLogger(ChallengeEvaluator.class);

    private final LockInChallengeRepository lockInChallengeRepository;
    private final DailyProgressRepository dailyProgressRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean evaluateOne(UUID challengeId, LocalDate yesterday) {
        LockInChallenge ch = lockInChallengeRepository.findById(challengeId).orElse(null);
        if (ch == null) return false;

        if (yesterday.equals(ch.getLastEvaluatedDate())) return false;
        if (yesterday.isBefore(ch.getStartDate())) return false;

        if (yesterday.isAfter(ch.getEndDate())) {
            ch.setStatus(ChallengeStatus.FAILED);
            ch.setLastEvaluatedDate(yesterday);
            lockInChallengeRepository.save(ch);
            log.info("ChallengeStreakScheduler: challenge {} past endDate, marked FAILED", ch.getId());
            return false;
        }

        DailyProgress dp = dailyProgressRepository.findByUserIdAndDate(ch.getUser().getId(), yesterday).orElse(null);
        boolean qualified = dp != null && (dp.getTopicsCreated() > 0 || dp.getRevisionsCompleted() > 0);

        if (qualified) {
            ch.setCurrentStreak(ch.getCurrentStreak() + 1);
        } else {
            int freezeRemaining = ch.getFreezeAllowed() - ch.getFreezeUsed();
            if (freezeRemaining > 0) {
                ch.setFreezeUsed(ch.getFreezeUsed() + 1);
            } else {
                ch.setStatus(ChallengeStatus.FAILED);
            }
        }

        int duration = ChallengeRules.durationDays(ch.getChallengeType());
        if (ch.getStatus() == ChallengeStatus.ACTIVE && ch.getCurrentStreak() >= duration) {
            ch.setStatus(ChallengeStatus.COMPLETED);
        }

        ch.setLastEvaluatedDate(yesterday);
        lockInChallengeRepository.save(ch);
        return true;
    }
}