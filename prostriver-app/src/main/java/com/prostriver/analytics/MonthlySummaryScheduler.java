package com.prostriver.analytics;

import com.prostriver.entity.User;
import com.prostriver.repository.UserRepository;
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
public class MonthlySummaryScheduler {

    private static final Logger log = LoggerFactory.getLogger(MonthlySummaryScheduler.class);
    private static final int BATCH_SIZE = 100;

    private final UserRepository userRepository;
    private final MonthlySummaryUpdater monthlySummaryUpdater;
    private final Clock clock;

    @Scheduled(cron = "0 30 1 * * *", zone = "Asia/Kolkata")
    public void updateMtdFromYesterday() {
        LocalDate yesterday = LocalDate.now(clock).minusDays(1);
        int month = yesterday.getMonthValue();
        int year = yesterday.getYear();
        LocalDate from = yesterday.withDayOfMonth(1);
        LocalDate to = yesterday;

        int totalProcessed = 0;
        int failures = 0;
        Pageable pageable = PageRequest.of(0, BATCH_SIZE);

        Page<User> page;
        do {
            page = userRepository.findAllByEmailVerifiedTrue(pageable);

            for (User user : page.getContent()) {
                try {
                    monthlySummaryUpdater.updateForUser(user, month, year, from, to);
                } catch (Exception e) {
                    log.error("MonthlySummary: failed for user {}", user.getId(), e);
                    failures++;
                }
            }

            totalProcessed += page.getNumberOfElements();
            pageable = page.nextPageable();
        } while (page.hasNext());

        log.info("MonthlySummary: done for {}-{}, {} users ({} failures)", year, month, totalProcessed, failures);
    }
}