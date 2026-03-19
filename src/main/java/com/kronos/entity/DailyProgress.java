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
                name = "uk_daily_progress_challenge_date",
                columnNames = {"lock_in_challenge_id", "date"}
        )
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
    @JoinColumn(name = "lock_in_challenge_id", nullable = false)
    private LockInChallenge lockInChallenge;

    @Column(nullable = false)
    private LocalDate date;

    @Column(nullable = false)
    private int dayNumber;

    private String summary;

    private int topicsStudied;

    private int topicsRevised;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}