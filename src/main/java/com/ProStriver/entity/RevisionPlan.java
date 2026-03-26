package com.ProStriver.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "revision_plans")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RevisionPlan {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true)
    private String name; // e.g., "1-4-7"

    private String description; // e.g., "Revise on Day 1, 4, and 7 for long-term retention"

    @Column(nullable = false)
    private String revisionDaysPattern; // e.g., "1,4,7"

    public List<Integer> getRevisionDays() {
        return Arrays.stream(revisionDaysPattern.split(","))
                .map(String::trim)
                .map(Integer::parseInt)
                .toList();
    }
}