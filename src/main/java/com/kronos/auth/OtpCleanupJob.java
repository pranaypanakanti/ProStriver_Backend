package com.kronos.auth;

import com.kronos.entity.enums.RefreshTokenStatus;
import com.kronos.repository.OtpCodeRepository;
import com.kronos.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Profile("worker")
@Component
@RequiredArgsConstructor
public class OtpCleanupJob {

    private final OtpCodeRepository otpCodeRepository;
    private final RefreshTokenRepository refreshTokenRepository;

    // every day at 03:15
    @Scheduled(cron = "0 15 3 * * *")
    @Transactional
    public void cleanup() {
        LocalDateTime now = LocalDateTime.now();

        // 1) OTP cleanup (always safe)
        otpCodeRepository.deleteByExpiresAtBefore(now);

        // 2) Ensure time-expired ACTIVE refresh tokens are marked EXPIRED
        refreshTokenRepository.markActiveTokensExpired(now);

        // 3) Delete expired refresh tokens
        refreshTokenRepository.deleteByExpiresAtBeforeAndStatus(now, RefreshTokenStatus.EXPIRED);

    }
}