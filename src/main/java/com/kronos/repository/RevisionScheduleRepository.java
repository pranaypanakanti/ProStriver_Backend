package com.kronos.repository;

import com.kronos.entity.RevisionSchedule;
import com.kronos.entity.enums.RevisionStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface RevisionScheduleRepository extends JpaRepository<RevisionSchedule, UUID> {

    List<RevisionSchedule> findAllByTopicIdOrderByScheduledDateAsc(UUID topicId);

    // Dashboard: all schedules for user on a date
    List<RevisionSchedule> findAllByTopicUserIdAndScheduledDateOrderByScheduledDateAsc(UUID userId, LocalDate scheduledDate);

    // Dashboard: pending schedules for user on a date
    List<RevisionSchedule> findAllByTopicUserIdAndStatusAndScheduledDateOrderByScheduledDateAsc(
            UUID userId,
            RevisionStatus status,
            LocalDate scheduledDate
    );

    // Batch job: find all pending schedules before today (to mark MISSED)
    List<RevisionSchedule> findAllByStatusAndScheduledDateBefore(RevisionStatus status, LocalDate date);

    // For notification jobs: find due schedules that haven't notified yet
    List<RevisionSchedule> findAllByStatusAndScheduledDateAndNotificationSentFalse(
            RevisionStatus status,
            LocalDate scheduledDate
    );
}