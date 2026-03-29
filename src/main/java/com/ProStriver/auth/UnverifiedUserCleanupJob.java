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

    // Every Sunday at 4:00 AM
    @Scheduled(cron = "0 0 4 * * SUN")
    @Transactional
    public void cleanupUnverifiedUsers() {
        List<User> unverified = userRepository.findAllByEmailVerifiedFalse();

        if (unverified.isEmpty()) {
            log.info("UnverifiedUserCleanup: no unverified users found");
            return;
        }

        for (User user : unverified) {
            otpCodeRepository.deleteAllByEmail(user.getEmail());

            refreshTokenRepository.deleteAllByUserId(user.getId());

            userRepository.delete(user);
        }

        log.info("UnverifiedUserCleanup: deleted {} unverified users", unverified.size());
    }
}