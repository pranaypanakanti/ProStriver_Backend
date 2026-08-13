package com.prostriver.analytics;

import com.fasterxml.jackson.core.type.TypeReference;
import com.prostriver.analytics.dto.AnalyticsOverviewResponse;
import com.prostriver.common.redis.RedisService;
import com.prostriver.common.exception.ApiException;
import com.prostriver.entity.DailyProgress;
import com.prostriver.entity.LockInChallenge;
import com.prostriver.entity.MonthlySummary;
import com.prostriver.entity.User;
import com.prostriver.entity.enums.ChallengeStatus;
import com.prostriver.repository.DailyProgressRepository;
import com.prostriver.repository.LockInChallengeRepository;
import com.prostriver.repository.MonthlySummaryRepository;
import com.prostriver.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AnalyticsService {

    private final UserRepository userRepository;
    private final MonthlySummaryRepository monthlySummaryRepository;
    private final LockInChallengeRepository lockInChallengeRepository;
    private final DailyProgressRepository dailyProgressRepository;
    private final RedisService redisService;

    private final Clock clock;

    @Transactional(readOnly = true)
    public AnalyticsOverviewResponse overview(String emailRaw) {
        User user = userRepository.findByEmail(emailRaw.toLowerCase().trim())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found"));

        String key = "analytics:overview:user:" + String.valueOf(user.getId());

        AnalyticsOverviewResponse response = redisService.get(key, new TypeReference<AnalyticsOverviewResponse>() {});

        if(response != null) {
            return response;
        }
        LocalDate today = LocalDate.now(clock);
        int month = today.getMonthValue();
        int year = today.getYear();

        MonthlySummary ms = monthlySummaryRepository.findByUserIdAndMonthAndYear(user.getId(), month, year)
                .orElseGet(() -> {
                    MonthlySummary x = new MonthlySummary();
                    x.setUser(user);
                    x.setMonth(month);
                    x.setYear(year);
                    x.setTotalTopicsLearned(0);
                    x.setTotalRevisionsCompleted(0);
                    x.setTotalRevisionsMissed(0);
                    return x;
                });

        int completed = ms.getTotalRevisionsCompleted();
        int missed = ms.getTotalRevisionsMissed();
        int emailed = completed + missed;
        double rate = (emailed == 0) ? 0.0 : ((double) completed) / emailed;

        AnalyticsOverviewResponse.RevisionGauge gauge =
                new AnalyticsOverviewResponse.RevisionGauge(completed, missed, emailed, rate, month, year);

        LockInChallenge active = lockInChallengeRepository.findByUserIdAndStatus(user.getId(), ChallengeStatus.ACTIVE)
                .orElse(null);

        AnalyticsOverviewResponse.ChallengeInfo challengeInfo = null;

        if (active != null) {
            int duration = com.prostriver.challenge.ChallengeRules.durationDays(active.getChallengeType());
            int freezeRemaining = Math.max(0, active.getFreezeAllowed() - active.getFreezeUsed());
            double progressPercent = (duration == 0) ? 0.0 : (100.0 * active.getCurrentStreak() / duration);

            LocalDate evaluatedThroughDate = LocalDate.now(clock).minusDays(1);

            LocalDate from = active.getStartDate();
            LocalDate to = active.getEndDate().isBefore(evaluatedThroughDate) ? active.getEndDate() : evaluatedThroughDate;

            int qualifiedDays = 0;
            double completionRate = 0.0;

            if (!to.isBefore(from) && duration > 0) {
                List<DailyProgress> window =
                        dailyProgressRepository.findAllByUserIdAndDateBetween(user.getId(), from, to);

                qualifiedDays = (int) window.stream()
                        .filter(dp -> dp.getTopicsCreated() > 0 || dp.getRevisionsCompleted() > 0)
                        .count();

                completionRate = ((double) qualifiedDays) / duration;
            }

            challengeInfo = new AnalyticsOverviewResponse.ChallengeInfo(
                    active.getId(),
                    active.getChallengeType(),
                    active.getStatus(),
                    duration,
                    active.getStartDate(),
                    active.getEndDate(),
                    evaluatedThroughDate,
                    qualifiedDays,
                    active.getCurrentStreak(),
                    active.getFreezeAllowed(),
                    active.getFreezeUsed(),
                    freezeRemaining,
                    progressPercent,
                    completionRate
            );
        }

        AnalyticsOverviewResponse overviewResponse = new AnalyticsOverviewResponse(gauge, challengeInfo);

        redisService.set(
                key,
                overviewResponse,
                10L
        );

        return overviewResponse;
    }

}