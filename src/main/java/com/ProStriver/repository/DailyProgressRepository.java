package com.ProStriver.repository;

import com.ProStriver.entity.DailyProgress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DailyProgressRepository extends JpaRepository<DailyProgress, UUID> {

    Optional<DailyProgress> findByUserIdAndDate(UUID userId, LocalDate date);

    List<DailyProgress> findAllByUserIdAndDateBetween(UUID userId, LocalDate from, LocalDate to);

    @Query("""
        select
          coalesce(sum(dp.topicsCreated), 0),
          coalesce(sum(dp.revisionsCompleted), 0),
          coalesce(sum(dp.revisionsEmailed - dp.revisionsCompleted), 0)
        from DailyProgress dp
        where dp.user.id = :userId
          and dp.date >= :from
          and dp.date <= :to
    """)
    Object[] sumMtd(@Param("userId") UUID userId,
                    @Param("from") LocalDate from,
                    @Param("to") LocalDate to);
}