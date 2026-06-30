package com.proStriver.auth;

import com.proStriver.entity.User;
import com.proStriver.repository.DailyProgressRepository;
import com.proStriver.repository.MonthlySummaryRepository;
import com.proStriver.repository.OtpCodeRepository;
import com.proStriver.repository.RefreshTokenRepository;
import com.proStriver.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Profile("worker")
@Component
@RequiredArgsConstructor
public class UnverifiedUserDeleter {

    private final UserRepository userRepository;
    private final OtpCodeRepository otpCodeRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final DailyProgressRepository dailyProgressRepository;
    private final MonthlySummaryRepository monthlySummaryRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void deleteOne(User user) {
        dailyProgressRepository.deleteAllByUserId(user.getId());
        monthlySummaryRepository.deleteAllByUserId(user.getId());
        otpCodeRepository.deleteAllByEmail(user.getEmail());
        refreshTokenRepository.deleteAllByUserId(user.getId());
        userRepository.delete(user);
    }
}