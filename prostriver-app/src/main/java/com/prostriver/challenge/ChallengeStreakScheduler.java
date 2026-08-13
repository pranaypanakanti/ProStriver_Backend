package com.prostriver.challenge;

import com.prostriver.entity.LockInChallenge;
import com.prostriver.entity.enums.ChallengeStatus;
import com.prostriver.repository.LockInChallengeRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

@Profile("worker")
@Component
@RequiredArgsConstructor
public class ChallengeStreakScheduler {

    private static final Logger log = LoggerFactory.getLogger(ChallengeStreakScheduler.class);

    private final LockInChallengeRepository lockInChallengeRepository;
    private final ChallengeEvaluator challengeEvaluator;
    private final Clock clock;

    @Scheduled(cron = "0 00 1 * * *", zone = "Asia/Kolkata")
    public void evaluateYesterday() {
        LocalDate yesterday = LocalDate.now(clock).minusDays(1);
        log.info("ChallengeStreakScheduler: evaluating {}", yesterday);

        List<LockInChallenge> active = lockInChallengeRepository.findByStatus(ChallengeStatus.ACTIVE);
        if (active.isEmpty()) {
            log.info("ChallengeStreakScheduler: no active challenges");
            return;
        }

        int evaluated = 0;
        int failures = 0;
        for (LockInChallenge ch : active) {
            try {
                if (challengeEvaluator.evaluateOne(ch.getId(), yesterday)) {
                    evaluated++;
                }
            } catch (Exception e) {
                log.error("ChallengeStreakScheduler: failed for challenge {}", ch.getId(), e);
                failures++;
            }
        }

        log.info("ChallengeStreakScheduler: evaluated {} challenges ({} failures)", evaluated, failures);
    }
}