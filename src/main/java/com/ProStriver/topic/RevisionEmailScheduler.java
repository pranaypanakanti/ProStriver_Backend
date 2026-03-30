package com.ProStriver.topic;

import com.ProStriver.entity.RevisionSchedule;
import com.ProStriver.entity.User;
import com.ProStriver.entity.enums.NotificationPreference;
import com.ProStriver.entity.enums.RevisionStatus;
import com.ProStriver.notification.EmailService;
import com.ProStriver.repository.RevisionScheduleRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Profile("worker")
@Component
@RequiredArgsConstructor
public class RevisionEmailScheduler {

    private static final Logger log = LoggerFactory.getLogger(RevisionEmailScheduler.class);

    private final RevisionScheduleRepository revisionScheduleRepository;
    private final EmailService emailService;
    private final Clock clock;

    @Scheduled(cron = "0 0 8 * * *", zone = "Asia/Kolkata")
    @Transactional
    public void sendDailyRevisionDigest() {
        LocalDate today = LocalDate.now(clock);
        log.info("RevisionEmailScheduler: starting digest for {}", today);

        List<RevisionSchedule> due = revisionScheduleRepository.findDueForEmail(today, RevisionStatus.PENDING);
        if (due.isEmpty()) {
            log.info("RevisionEmailScheduler: no revisions due for {}", today);
            return;
        }

        log.info("RevisionEmailScheduler: found {} due revisions", due.size());

        Map<User, List<RevisionSchedule>> byUser = due.stream()
                .collect(Collectors.groupingBy(rs -> rs.getTopic().getUser()));

        List<RevisionSchedule> sent = new ArrayList<>();

        for (Map.Entry<User, List<RevisionSchedule>> entry : byUser.entrySet()) {
            User user = entry.getKey();
            List<RevisionSchedule> items = entry.getValue();

            if (user.getNotificationPreference() == NotificationPreference.NONE) {
                sent.addAll(items);
                log.info("RevisionEmailScheduler: skipped {} (preference=NONE), {} items",
                        user.getEmail(), items.size());
                continue;
            }

            try {
                String subject = "ProStriver - Topics to revise today (" + today + ")";
                String body = buildDigestBody(items, today);
                emailService.sendReminder(user.getEmail(), subject, body);
                sent.addAll(items);
                log.info("RevisionEmailScheduler: sent to {} ({} items)", user.getEmail(), items.size());
            } catch (Exception e) {
                // Per-user isolation: other users still get their emails
                log.error("RevisionEmailScheduler: failed to send to {}", user.getEmail(), e);
            }
        }

        sent.forEach(rs -> rs.setNotificationSent(true));
        revisionScheduleRepository.saveAll(sent);

        log.info("RevisionEmailScheduler: completed. Notified for {}/{} schedules", sent.size(), due.size());
    }

    private String buildDigestBody(List<RevisionSchedule> items, LocalDate today) {
        StringBuilder sb = new StringBuilder();
        sb.append("Hello,\n\n");
        sb.append("Here are your topics to revise today (").append(today).append("):\n\n");

        int i = 1;
        for (RevisionSchedule rs : items) {
            sb.append(i++).append(") ")
                    .append(rs.getTopic().getSubject())
                    .append(" - ")
                    .append(rs.getTopic().getTitle())
                    .append(" (Day ").append(rs.getDayNumber()).append(")\n");
        }

        sb.append("\nOpen ProStriver to mark revisions as completed.\n\n");
        sb.append("Regards,\nProStriver Team\n");
        return sb.toString();
    }
}