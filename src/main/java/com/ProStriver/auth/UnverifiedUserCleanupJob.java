package com.ProStriver.auth;

import com.ProStriver.entity.User;
import com.ProStriver.repository.OtpCodeRepository;
import com.ProStriver.repository.RefreshTokenRepository;
import com.ProStriver.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Profile("worker")
@Component
@RequiredArgsConstructor
public class UnverifiedUserCleanupJob {

    private static final Logger log = LoggerFactory.getLogger(UnverifiedUserCleanupJob.class);

    private final UserRepository userRepository;
    private final OtpCodeRepository otpCodeRepository;
    private final RefreshTokenRepository refreshTokenRepository;

    @Scheduled(cron = "0 0 4 * * SUN", zone = "Asia/Kolkata")
    @Transactional
    public void cleanupUnverifiedUsers() {
        List<User> unverified = userRepository.findAllByEmailVerifiedFalse();

        if (unverified.isEmpty()) {
            log.info("UnverifiedUserCleanup: no unverified users found");
            return;
        }

        int deleted = 0;
        for (User user : unverified) {
            try {
                otpCodeRepository.deleteAllByEmail(user.getEmail());
                refreshTokenRepository.deleteAllByUserId(user.getId());
                userRepository.delete(user);
                userRepository.flush(); // force FK check immediately
                deleted++;
            } catch (Exception ex) {
                log.warn("UnverifiedUserCleanup: skipped user {} ({}): {}",
                        user.getId(), user.getEmail(), ex.getMessage());
            }
        }

        log.info("UnverifiedUserCleanup: deleted {}/{} unverified users", deleted, unverified.size());
    }
}