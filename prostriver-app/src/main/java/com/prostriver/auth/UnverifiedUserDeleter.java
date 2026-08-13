package com.prostriver.auth;

import com.prostriver.entity.User;
import com.prostriver.repository.DailyProgressRepository;
import com.prostriver.repository.MonthlySummaryRepository;
import com.prostriver.repository.OtpCodeRepository;
import com.prostriver.repository.RefreshTokenRepository;
import com.prostriver.repository.UserRepository;
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