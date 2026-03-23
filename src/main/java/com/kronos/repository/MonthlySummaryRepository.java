package com.kronos.repository;

import com.kronos.entity.MonthlySummary;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface MonthlySummaryRepository extends JpaRepository<MonthlySummary, UUID> {
    Optional<MonthlySummary> findByUserIdAndMonthAndYear(UUID userId, int month, int year);
}