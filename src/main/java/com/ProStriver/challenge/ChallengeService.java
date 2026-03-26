package com.ProStriver.challenge;

import com.ProStriver.common.exception.ApiException;
import com.ProStriver.entity.DailyProgress;
import com.ProStriver.entity.LockInChallenge;
import com.ProStriver.entity.User;
import com.ProStriver.entity.enums.ChallengeStatus;
import com.ProStriver.entity.enums.ChallengeType;
import com.ProStriver.repository.DailyProgressRepository;
import com.ProStriver.repository.LockInChallengeRepository;
import com.ProStriver.repository.UserRepository;
import com.ProStriver.challenge.dto.ChallengeMeResponse;
import com.ProStriver.challenge.dto.ChallengePlanResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ChallengeService {

    private final UserRepository userRepository;
    private final LockInChallengeRepository lockInChallengeRepository;
    private final DailyProgressRepository dailyProgressRepository;

    private final Clock clock;

    public List<ChallengePlanResponse> plans() {
        return List.of(
                plan(ChallengeType.THIRTY_DAY),
                plan(ChallengeType.HUNDRED_DAY),
                plan(ChallengeType.TWO_HUNDRED_DAY),
                plan(ChallengeType.THREE_SIXTY_FIVE_DAY)
        );
    }

    @Transactional
    public ChallengeMeResponse select(String emailRaw, ChallengeType type) {
        User user = getUser(emailRaw);

        lockInChallengeRepository.findByUserIdAndStatus(user.getId(), ChallengeStatus.ACTIVE)
                .ifPresent(active -> {
                    throw new ApiException(HttpStatus.BAD_REQUEST, "An ACTIVE challenge already exists. Quit it before selecting a new one.");
                });

        int duration = ChallengeRules.durationDays(type);
        int freezeAllowed = ChallengeRules.freezeAllowed(type);

        LocalDate start = LocalDate.now(clock);
        LocalDate end = start.plusDays(duration - 1L);

        LockInChallenge ch = new LockInChallenge();
        ch.setUser(user);
        ch.setChallengeType(type);
        ch.setStartDate(start);
        ch.setEndDate(end);
        ch.setStatus(ChallengeStatus.ACTIVE);
        ch.setCurrentStreak(0);
        ch.setFreezeAllowed(freezeAllowed);
        ch.setFreezeUsed(0);
        ch.setLastEvaluatedDate(null);

        lockInChallengeRepository.save(ch);

        return toMeResponse(ch);
    }

    @Transactional(readOnly = true)
    public ChallengeMeResponse me(String emailRaw) {
        User user = getUser(emailRaw);

        LockInChallenge ch = lockInChallengeRepository.findByUserIdAndStatus(user.getId(), ChallengeStatus.ACTIVE)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "No ACTIVE challenge found"));

        return toMeResponse(ch);
    }

    @Transactional
    public void quit(String emailRaw) {
        User user = getUser(emailRaw);

        LockInChallenge ch = lockInChallengeRepository.findByUserIdAndStatus(user.getId(), ChallengeStatus.ACTIVE)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "No ACTIVE challenge found"));

        ch.setStatus(ChallengeStatus.QUIT);
        lockInChallengeRepository.save(ch);
    }

    private ChallengePlanResponse plan(ChallengeType type) {
        int d = ChallengeRules.durationDays(type);
        int freezeAllowed = ChallengeRules.freezeAllowed(type);
        return new ChallengePlanResponse(type, d, freezeAllowed);
    }

    private ChallengeMeResponse toMeResponse(LockInChallenge ch) {
        int duration = ChallengeRules.durationDays(ch.getChallengeType());
        int freezeRemaining = Math.max(0, ch.getFreezeAllowed() - ch.getFreezeUsed());

        double progressPercent = (duration == 0) ? 0.0 : (100.0 * ch.getCurrentStreak() / duration);

        LocalDate yesterday = LocalDate.now(clock).minusDays(1);
        LocalDate from = ch.getStartDate();
        LocalDate to = ch.getEndDate().isBefore(yesterday) ? ch.getEndDate() : yesterday;

        double completionRate = 0.0;
        if (!to.isBefore(from) && duration > 0) {
            List<DailyProgress> window =
                    dailyProgressRepository.findAllByUserIdAndDateBetween(ch.getUser().getId(), from, to);

            long qualifiedDays = window.stream()
                    .filter(dp -> dp.getTopicsCreated() > 0 || dp.getRevisionsCompleted() > 0)
                    .count();

            completionRate = ((double) qualifiedDays) / duration;
        }

        return new ChallengeMeResponse(
                ch.getId(),
                ch.getChallengeType(),
                ch.getStatus(),
                duration,
                ch.getStartDate(),
                ch.getEndDate(),
                ch.getCurrentStreak(),
                ch.getFreezeAllowed(),
                ch.getFreezeUsed(),
                freezeRemaining,
                progressPercent,
                completionRate
        );
    }

    private User getUser(String emailRaw) {
        return userRepository.findByEmail(emailRaw.toLowerCase().trim())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found"));
    }
}