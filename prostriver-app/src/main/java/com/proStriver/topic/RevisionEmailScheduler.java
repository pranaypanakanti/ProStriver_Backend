package com.proStriver.topic;

import com.proStriver.entity.enums.NotificationPreference;
import com.proStriver.notification.EmailService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Profile("worker")
@Component
@RequiredArgsConstructor
public class RevisionEmailScheduler {

    private static final Logger log = LoggerFactory.getLogger(RevisionEmailScheduler.class);

    private final RevisionDigestService revisionDigestService;
    private final EmailService emailService;
    private final Clock clock;

    @Scheduled(cron = "0 0 8 * * *", zone = "Asia/Kolkata")
    public void sendDailyRevisionDigest() {
        LocalDate today = LocalDate.now(clock);
        log.info("RevisionEmailScheduler: starting digest for {}", today);

        List<RevisionDigestData> digests = revisionDigestService.loadDueGrouped(today);
        if (digests.isEmpty()) {
            log.info("RevisionEmailScheduler: no revisions due for {}", today);
            return;
        }

        int dueCount = digests.stream().mapToInt(d -> d.items().size()).sum();
        log.info("RevisionEmailScheduler: found {} due revisions", dueCount);

        List<UUID> sentIds = new ArrayList<>();

        for (RevisionDigestData d : digests) {
            if (d.preference() == NotificationPreference.NONE) {
                sentIds.addAll(d.scheduleIds());
                log.info("RevisionEmailScheduler: skipped {} (preference=NONE), {} items",
                        d.email(), d.items().size());
                continue;
            }
            try {
                String subject = "ProStriver - Topics to revise today (" + today + ")";
                String body = buildDigestBody(d.items(), today);
                emailService.sendReminder(d.email(), subject, body);
                sentIds.addAll(d.scheduleIds());
                log.info("RevisionEmailScheduler: sent to {} ({} items)", d.email(), d.items().size());
            } catch (Exception e) {
                log.error("RevisionEmailScheduler: failed to send to {}", d.email(), e);
            }
        }

        revisionDigestService.markNotified(sentIds);
        log.info("RevisionEmailScheduler: completed. Notified for {}/{} schedules", sentIds.size(), dueCount);
    }

    private String buildDigestBody(List<RevisionDigestData.Item> items, LocalDate today) {
        StringBuilder sb = new StringBuilder();
        sb.append("Hello,\n\n");
        sb.append("Here are your topics to revise today (").append(today).append("):\n\n");

        int i = 1;
        for (RevisionDigestData.Item item : items) {
            sb.append(i++).append(") ")
                    .append(item.subject())
                    .append(" - ")
                    .append(item.title())
                    .append(" (Day ").append(item.dayNumber()).append(")\n");
        }

        sb.append("\nOpen prostriver.me to mark revisions as completed.\n\n");
        sb.append("Regards,\nProStriver Team\n");
        return sb.toString();
    }
}