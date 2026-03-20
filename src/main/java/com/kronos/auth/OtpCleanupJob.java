package com.kronos.auth;

import com.kronos.repository.OtpCodeRepository;
import com.kronos.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class OtpCleanupJob {

    private final OtpCodeRepository otpCodeRepository;
    private final RefreshTokenRepository refreshTokenRepository;

    // every day at 03:15
    @Scheduled(cron = "0 15 3 * * *")
    public void cleanup() {
        otpCodeRepository.deleteByExpiresAtBefore(LocalDateTime.now());
        refreshTokenRepository.deleteByExpiresAtBefore(LocalDateTime.now());
    }
}