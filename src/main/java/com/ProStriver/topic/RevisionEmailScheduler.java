package com.ProStriver.topic;

import com.ProStriver.entity.RevisionSchedule;
import com.ProStriver.entity.User;
import com.ProStriver.entity.enums.RevisionStatus;
import com.ProStriver.notification.EmailService;
import com.ProStriver.repository.RevisionScheduleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Profile("worker")
@Component
@RequiredArgsConstructor
public class RevisionEmailScheduler {

    private final RevisionScheduleRepository revisionScheduleRepository;
    private final EmailService emailService;

    private final Clock clock;

    @Scheduled(cron = "0 0 8 * * *")
    @Transactional
    public void sendDailyRevisionDigest() {
        LocalDate today = LocalDate.now(clock);

        List<RevisionSchedule> due = revisionScheduleRepository.findDueForEmail(today, RevisionStatus.PENDING);
        if (due.isEmpty()) return;

        Map<User, List<RevisionSchedule>> byUser = due.stream()
                .collect(Collectors.groupingBy(rs -> rs.getTopic().getUser()));

        for (Map.Entry<User, List<RevisionSchedule>> entry : byUser.entrySet()) {
            User user = entry.getKey();
            List<RevisionSchedule> items = entry.getValue();

            String subject = "ProStriver - Topics to revise today (" + today + ")";
            String body = buildDigestBody(items, today);

            emailService.sendReminder(user.getEmail(), subject, body);
        }

        due.forEach(rs -> rs.setNotificationSent(true));
        revisionScheduleRepository.saveAll(due);
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