package com.kronos.entity;

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
        name = "daily_progress",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_daily_progress_user_date",
                columnNames = {"user_id", "date"}
        ),
        indexes = {
                @Index(name = "idx_daily_progress_user_date", columnList = "user_id,date")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DailyProgress {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private LocalDate date;

    @Column(nullable = false)
    private int topicsCreated = 0;

    @Column(nullable = false)
    private int revisionsEmailed = 0;

    @Column(nullable = false)
    private int revisionsCompleted = 0;

    @Column(nullable = false)
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