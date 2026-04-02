package com.ProStriver.auth;

import com.ProStriver.entity.enums.RefreshTokenStatus;
import com.ProStriver.repository.OtpCodeRepository;
import com.ProStriver.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;

@Profile("worker")
@Component
@RequiredArgsConstructor
public class OtpCleanupJob {

    private static final Logger log = LoggerFactory.getLogger(OtpCleanupJob.class);
    private static final int REVOKED_RETENTION_DAYS = 2;

    private final OtpCodeRepository otpCodeRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final Clock clock;

    @Scheduled(cron = "0 15 3 * * *", zone = "Asia/Kolkata")
    @Transactional
    public void cleanup() {
        LocalDateTime now = LocalDateTime.now(clock);
        log.info("OtpCleanupJob: starting cleanup at {}", now);

        otpCodeRepository.deleteByExpiresAtBefore(now);
        log.info("OtpCleanupJob: expired OTPs deleted");

        int marked = refreshTokenRepository.markActiveTokensExpired(now);
        log.info("OtpCleanupJob: marked {} active tokens as expired", marked);

        refreshTokenRepository.deleteByExpiresAtBeforeAndStatus(now, RefreshTokenStatus.EXPIRED);
        log.info("OtpCleanupJob: expired refresh tokens deleted");

        LocalDateTime revokedCutoff = now.minusDays(REVOKED_RETENTION_DAYS);
        refreshTokenRepository.deleteByRevokedAtBeforeAndStatus(revokedCutoff, RefreshTokenStatus.REVOKED);
        log.info("OtpCleanupJob: revoked refresh tokens (older than {} days) deleted", REVOKED_RETENTION_DAYS);
    }
}