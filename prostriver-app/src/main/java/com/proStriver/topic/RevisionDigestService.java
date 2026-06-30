package com.proStriver.topic;

import com.proStriver.entity.RevisionSchedule;
import com.proStriver.entity.User;
import com.proStriver.entity.enums.RevisionStatus;
import com.proStriver.repository.RevisionScheduleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Profile("worker")
@Service
@RequiredArgsConstructor
public class RevisionDigestService {

    private final RevisionScheduleRepository revisionScheduleRepository;

    @Transactional(readOnly = true)
    public List<RevisionDigestData> loadDueGrouped(LocalDate today) {
        List<RevisionSchedule> due = revisionScheduleRepository.findDueForEmail(today, RevisionStatus.PENDING);
        if (due.isEmpty()) return List.of();

        Map<User, List<RevisionSchedule>> byUser = due.stream()
                .collect(Collectors.groupingBy(rs -> rs.getTopic().getUser()));

        List<RevisionDigestData> result = new ArrayList<>();
        for (Map.Entry<User, List<RevisionSchedule>> entry : byUser.entrySet()) {
            User user = entry.getKey();
            List<RevisionDigestData.Item> items = entry.getValue().stream()
                    .map(rs -> new RevisionDigestData.Item(
                            rs.getId(),
                            rs.getTopic().getSubject(),
                            rs.getTopic().getTitle(),
                            rs.getDayNumber()))
                    .toList();
            result.add(new RevisionDigestData(user.getEmail(), user.getNotificationPreference(), items));
        }
        return result;
    }

    @Transactional
    public void markNotified(List<UUID> scheduleIds) {
        if (scheduleIds.isEmpty()) return;
        List<RevisionSchedule> schedules = revisionScheduleRepository.findAllById(scheduleIds);
        schedules.forEach(rs -> rs.setNotificationSent(true));
        revisionScheduleRepository.saveAll(schedules);
    }
}