package com.proStriver.analytics;

import com.proStriver.entity.MonthlySummary;
import com.proStriver.entity.User;
import com.proStriver.repository.DailyProgressRepository;
import com.proStriver.repository.MonthlySummaryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Profile("worker")
@Component
@RequiredArgsConstructor
public class MonthlySummaryUpdater {

    private final DailyProgressRepository dailyProgressRepository;
    private final MonthlySummaryRepository monthlySummaryRepository;
    private final Clock clock;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void updateForUser(User user, int month, int year, LocalDate from, LocalDate to) {
        Object[] sums = unwrapRow(dailyProgressRepository.sumMtd(user.getId(), from, to));

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

    private Object[] unwrapRow(Object[] raw) {
        if (raw == null) return null;
        if (raw.length == 1 && raw[0] instanceof Object[]) {
            return (Object[]) raw[0];
        }
        return raw;
    }

    private int safeInt(Object[] arr, int index) {
        if (arr == null || index >= arr.length || arr[index] == null) return 0;
        return ((Number) arr[index]).intValue();
    }
}