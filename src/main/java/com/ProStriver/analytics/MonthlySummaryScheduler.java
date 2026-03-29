package com.ProStriver.analytics;

import com.ProStriver.entity.MonthlySummary;
import com.ProStriver.entity.User;
import com.ProStriver.repository.DailyProgressRepository;
import com.ProStriver.repository.MonthlySummaryRepository;
import com.ProStriver.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Profile("worker")
@Component
@RequiredArgsConstructor
public class MonthlySummaryScheduler {

    private static final Logger log = LoggerFactory.getLogger(MonthlySummaryScheduler.class);
    private static final int BATCH_SIZE = 100;

    private final UserRepository userRepository;
    private final DailyProgressRepository dailyProgressRepository;
    private final MonthlySummaryRepository monthlySummaryRepository;

    private final Clock clock;

    @Scheduled(cron = "0 30 0 * * *")
    @Transactional
    public void updateMtdFromYesterday() {
        LocalDate yesterday = LocalDate.now(clock).minusDays(1);
        int month = yesterday.getMonthValue();
        int year = yesterday.getYear();

        LocalDate from = yesterday.withDayOfMonth(1);
        LocalDate to = yesterday;

        int totalProcessed = 0;
        Pageable pageable = PageRequest.of(0, BATCH_SIZE);

        Page<User> page;
        do {
            page = userRepository.findAll(pageable);

            for (User user : page.getContent()) {
                Object[] sums = dailyProgressRepository.sumMtd(user.getId(), from, to);

                int topicsCreatedMtd = safeInt(sums, 0);
                int revisionsCompletedMtd = safeInt(sums, 1);
                int revisionsMissedMtd = safeInt(sums, 2);

                MonthlySummary ms = monthlySummaryRepository.findByUserIdAndMonthAndYear(user.getId(), month, year)
                        .orElseGet(() -> {
                            MonthlySummary x = new MonthlySummary();
                            x.setUser(user);
                            x.setMonth(month);
                            x.setYear(year);
                            return x;
                        });

                ms.setTotalTopicsLearned(topicsCreatedMtd);
                ms.setTotalRevisionsCompleted(revisionsCompletedMtd);
                ms.setTotalRevisionsMissed(revisionsMissedMtd);
                ms.setGeneratedAt(LocalDateTime.now(clock));

                monthlySummaryRepository.save(ms);
            }

            totalProcessed += page.getNumberOfElements();
            pageable = page.nextPageable();
        } while (page.hasNext());

        log.info("MonthlySummary: done for {}-{}, {} users", year, month, totalProcessed);
    }

    private int safeInt(Object[] arr, int index) {
        if (arr == null || index >= arr.length || arr[index] == null) return 0;
        return ((Number) arr[index]).intValue();
    }
}