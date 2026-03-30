package com.ProStriver.challenge;

import com.ProStriver.entity.DailyProgress;
import com.ProStriver.entity.LockInChallenge;
import com.ProStriver.entity.enums.ChallengeStatus;
import com.ProStriver.repository.DailyProgressRepository;
import com.ProStriver.repository.LockInChallengeRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

@Profile("worker")
@Component
@RequiredArgsConstructor
public class ChallengeStreakScheduler {

    private static final Logger log = LoggerFactory.getLogger(ChallengeStreakScheduler.class);

    private final LockInChallengeRepository lockInChallengeRepository;
    private final DailyProgressRepository dailyProgressRepository;

    private final Clock clock;

    @Scheduled(cron = "0 00 1 * * *", zone = "Asia/Kolkata")
    @Transactional
    public void evaluateYesterday() {
        LocalDate yesterday = LocalDate.now(clock).minusDays(1);
        log.info("ChallengeStreakScheduler: evaluating {}", yesterday);

        List<LockInChallenge> active = lockInChallengeRepository.findByStatus(ChallengeStatus.ACTIVE);
        if (active.isEmpty()) {
            log.info("ChallengeStreakScheduler: no active challenges");
            return;
        }

        int evaluated = 0;
        for (LockInChallenge ch : active) {
            // Skip if already evaluated for this date
            if (yesterday.equals(ch.getLastEvaluatedDate())) continue;

            // Skip days before the challenge started
            if (yesterday.isBefore(ch.getStartDate())) continue;

            // Skip if challenge has already passed its end date
            if (yesterday.isAfter(ch.getEndDate())) {
                ch.setStatus(ChallengeStatus.FAILED);
                ch.setLastEvaluatedDate(yesterday);
                lockInChallengeRepository.save(ch);
                log.info("ChallengeStreakScheduler: challenge {} past endDate, marked FAILED", ch.getId());
                continue;
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
            evaluated++;
        }

        log.info("ChallengeStreakScheduler: evaluated {} challenges", evaluated);
    }
}