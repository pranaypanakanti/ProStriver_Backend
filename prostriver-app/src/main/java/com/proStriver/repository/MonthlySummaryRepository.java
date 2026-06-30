package com.proStriver.repository;

import com.proStriver.entity.MonthlySummary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface MonthlySummaryRepository extends JpaRepository<MonthlySummary, UUID> {

    @Modifying
    @Query("delete from MonthlySummary m where m.user.id = :userId")
    void deleteAllByUserId(@Param("userId") UUID userId);

    Optional<MonthlySummary> findByUserIdAndMonthAndYear(UUID userId, int month, int year);
}