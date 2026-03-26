package com.ProStriver.analytics;

import com.ProStriver.entity.MonthlySummary;
import com.ProStriver.entity.User;
import com.ProStriver.repository.DailyProgressRepository;
import com.ProStriver.repository.MonthlySummaryRepository;
import com.ProStriver.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Profile("worker")
@Component
@RequiredArgsConstructor
public class MonthlySummaryScheduler {

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

        List<User> users = userRepository.findAll();

        for (User user : users) {
            Object[] sums = dailyProgressRepository.sumMtd(user.getId(), from, to);

            int topicsCreatedMtd = ((Number) sums[0]).intValue();
            int revisionsCompletedMtd = ((Number) sums[1]).intValue();
            int revisionsMissedMtd = ((Number) sums[2]).intValue();

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
    }
}