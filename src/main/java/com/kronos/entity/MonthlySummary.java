package com.kronos.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "monthly_summaries",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_monthly_summary_user_month_year",
                columnNames = {"user_id", "month", "year"}
        )
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MonthlySummary {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private int month;

    @Column(nullable = false)
    private int year;

    private int totalTopicsLearned;

    private int totalRevisionsCompleted;

    private int totalRevisionsMissed;

    @Column(columnDefinition = "TEXT")
    private String aiSummary;

    @Column(nullable = false)
    private LocalDateTime generatedAt;

    @PrePersist
    protected void onCreate() {
        this.generatedAt = LocalDateTime.now();
    }
}