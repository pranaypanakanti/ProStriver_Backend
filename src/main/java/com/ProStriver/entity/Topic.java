package com.ProStriver.entity;

import com.ProStriver.entity.enums.TopicStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "topics",
        indexes = {
                @Index(name = "idx_topics_user_created_at", columnList = "user_id,created_at"),
                @Index(name = "idx_topics_user_status", columnList = "user_id,status"),
                @Index(name = "idx_topics_revision_plan_id", columnList = "revision_plan_id")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Topic {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private String subject;

    @Column(nullable = false)
    private String title;

    private String notes;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "revision_plan_id")
    private RevisionPlan revisionPlan;

    @Column(name = "manual_reminder_pattern")
    private String manualReminderPattern;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TopicStatus status;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}