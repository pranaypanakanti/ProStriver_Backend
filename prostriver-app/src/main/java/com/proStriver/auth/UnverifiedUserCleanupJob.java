package com.proStriver.auth;

import com.proStriver.entity.User;
import com.proStriver.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Profile("worker")
@Component
@RequiredArgsConstructor
public class UnverifiedUserCleanupJob {

    private static final Logger log = LoggerFactory.getLogger(UnverifiedUserCleanupJob.class);

    private final UserRepository userRepository;
    private final UnverifiedUserDeleter unverifiedUserDeleter;

    @Scheduled(cron = "0 0 4 * * SUN", zone = "Asia/Kolkata")
    public void cleanupUnverifiedUsers() {
        List<User> unverified = userRepository.findAllByEmailVerifiedFalse();

        if (unverified.isEmpty()) {
            log.info("UnverifiedUserCleanup: no unverified users found");
            return;
        }

        int deleted = 0;
        for (User user : unverified) {
            try {
                unverifiedUserDeleter.deleteOne(user);
                deleted++;
            } catch (Exception ex) {
                log.warn("UnverifiedUserCleanup: skipped user {} ({}): {}",
                        user.getId(), user.getEmail(), ex.getMessage());
            }
        }

        log.info("UnverifiedUserCleanup: deleted {}/{} unverified users", deleted, unverified.size());
    }
}