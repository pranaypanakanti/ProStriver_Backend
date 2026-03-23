package com.kronos.challenge;

import com.kronos.entity.DailyProgress;
import com.kronos.entity.LockInChallenge;
import com.kronos.entity.enums.ChallengeStatus;
import com.kronos.entity.enums.ChallengeType;
import com.kronos.repository.DailyProgressRepository;
import com.kronos.repository.LockInChallengeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

@Component
@RequiredArgsConstructor
public class ChallengeStreakScheduler {

    private final LockInChallengeRepository lockInChallengeRepository;
    private final DailyProgressRepository dailyProgressRepository;

    private final Clock clock; // NEW

    @Scheduled(cron = "0 20 0 * * *")
    @Transactional
    public void evaluateYesterday() {
        LocalDate yesterday = LocalDate.now(clock).minusDays(1);

        List<LockInChallenge> active = lockInChallengeRepository.findByStatus(ChallengeStatus.ACTIVE);
        if (active.isEmpty()) return;

        for (LockInChallenge ch : active) {
            if (yesterday.equals(ch.getLastEvaluatedDate())) continue;

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
        }
    }

}