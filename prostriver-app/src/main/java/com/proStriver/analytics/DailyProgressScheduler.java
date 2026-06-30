package com.proStriver.analytics;

import com.proStriver.entity.User;
import com.proStriver.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDate;

@Profile("worker")
@Component
@RequiredArgsConstructor
public class DailyProgressScheduler {

    private static final Logger log = LoggerFactory.getLogger(DailyProgressScheduler.class);
    private static final int BATCH_SIZE = 100;

    private final UserRepository userRepository;
    private final DailyProgressComputer dailyProgressComputer;
    private final Clock clock;

    @Scheduled(cron = "0 30 0 * * *", zone = "Asia/Kolkata")
    public void computeYesterday() {
        LocalDate yesterday = LocalDate.now(clock).minusDays(1);

        int totalProcessed = 0;
        int failures = 0;
        Pageable pageable = PageRequest.of(0, BATCH_SIZE);

        Page<User> page;
        do {
            page = userRepository.findAllByEmailVerifiedTrue(pageable);

            for (User user : page.getContent()) {
                try {
                    dailyProgressComputer.computeForUser(user, yesterday);
                } catch (Exception e) {
                    log.error("DailyProgress: failed for user {}", user.getId(), e);
                    failures++;
                }
            }

            totalProcessed += page.getNumberOfElements();
            pageable = page.nextPageable();
        } while (page.hasNext());

        log.info("DailyProgress: computed {} for {} users ({} failures)", yesterday, totalProcessed, failures);
    }
}