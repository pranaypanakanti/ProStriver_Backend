package com.kronos.entity;

import com.kronos.entity.enums.RevisionStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "revision_schedules",
        indexes = {
                @Index(name = "idx_rev_sched_topic_id", columnList = "topic_id"),
                @Index(name = "idx_rev_sched_scheduled_date_status", columnList = "scheduled_date,status"),
                @Index(name = "idx_rev_sched_notify", columnList = "scheduled_date,status,notification_sent"),
                @Index(name = "idx_rev_sched_status_scheduled_date", columnList = "status,scheduled_date")
        },
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_rev_sched_topic_day_number",
                        columnNames = {"topic_id", "day_number"}
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RevisionSchedule {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "topic_id", nullable = false)
    private Topic topic;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "revision_plan_id", nullable = false)
    private RevisionPlan revisionPlan;

    @Column(nullable = false)
    private int dayNumber;

    @Column(nullable = false)
    private LocalDate scheduledDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RevisionStatus status;

    private LocalDateTime completedAt;

    private String googleCalendarEventId;

    @Column(nullable = false)
    private boolean notificationSent = false;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}