package com.kronos.repository;

import com.kronos.entity.DailyProgress;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DailyProgressRepository extends JpaRepository<DailyProgress, UUID> {

    Optional<DailyProgress> findByLockInChallengeIdAndDate(UUID lockInChallengeId, LocalDate date);

    List<DailyProgress> findAllByLockInChallengeIdOrderByDateAsc(UUID lockInChallengeId);

    boolean existsByLockInChallengeIdAndDate(UUID lockInChallengeId, LocalDate date);
}