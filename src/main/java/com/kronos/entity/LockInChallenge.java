package com.kronos.entity;

import com.kronos.entity.enums.ChallengeStatus;
import com.kronos.entity.enums.ChallengeType;
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
        name = "lock_in_challenges",
        indexes = {
                @Index(name = "idx_lock_in_challenges_user_status", columnList = "user_id,status")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class LockInChallenge {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ChallengeType challengeType;

    @Column(nullable = false)
    private LocalDate startDate;

    @Column(nullable = false)
    private LocalDate endDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ChallengeStatus status;

    @Column(nullable = false)
    private int currentStreak;

    @Column(nullable = false)
    private int freezeAllowed;

    @Column(nullable = false)
    private int freezeUsed;

    private LocalDate lastEvaluatedDate;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}