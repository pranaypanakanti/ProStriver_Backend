package com.kronos.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "badges")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Badge {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lock_in_challenge_id", nullable = false, unique = true)
    private LockInChallenge lockInChallenge;

    @Column(nullable = false)
    private String title; // e.g., " 30-Day Warrior ", "100-Day Legend "

    @Column(columnDefinition = "TEXT")
    private String summary; // AI-generated summary of topics focused during the challenge

    private String badgeImageUrl;

    @Column(nullable = false)
    private LocalDateTime earnedAt;

    @PrePersist
    protected void onCreate() {
        this.earnedAt = LocalDateTime.now();
    }
}