package com.ProStriver.repository;

import com.ProStriver.entity.RevisionSchedule;
import com.ProStriver.entity.enums.RevisionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RevisionScheduleRepository extends JpaRepository<RevisionSchedule, UUID> {

    @Query("""
            select rs from RevisionSchedule rs
            join fetch rs.topic t
            join fetch t.user u
            where rs.scheduledDate = :date
              and rs.status = :status
              and rs.notificationSent = false
              and rs.deletedAt is null
              and t.deletedAt is null
              and t.status = 'ACTIVE'
            """)
    List<RevisionSchedule> findDueForEmail(LocalDate date, RevisionStatus status);

    @Query("""
            select rs from RevisionSchedule rs
            join fetch rs.topic t
            where rs.scheduledDate = :date
              and rs.status = :status
              and rs.deletedAt is null
              and t.deletedAt is null
              and t.user.id = :userId
              and t.status = 'ACTIVE'
            order by t.createdAt desc
            """)
    List<RevisionSchedule> findTodayForUser(UUID userId, LocalDate date, RevisionStatus status);

    @Query("""
            select rs from RevisionSchedule rs
            where rs.id = :id
              and rs.deletedAt is null
              and rs.topic.deletedAt is null
              and rs.topic.user.id = :userId
            """)
    Optional<RevisionSchedule> findActiveByIdAndUserId(UUID id, UUID userId);

    @Query("""
            select rs from RevisionSchedule rs
            where rs.topic.id = :topicId
              and rs.deletedAt is null
            """)
    List<RevisionSchedule> findAllActiveByTopicId(UUID topicId);

    @Query("""
    select rs from RevisionSchedule rs
    where rs.deletedAt is null
      and rs.topic.deletedAt is null
      and rs.scheduledDate = :date
      and rs.notificationSent = true
      and rs.status = :status
""")
    List<RevisionSchedule> findAllEmailedPendingForDate(@Param("date") LocalDate date,
                                                        @Param("status") RevisionStatus status);

    @Query("""
    select count(rs) from RevisionSchedule rs
    where rs.deletedAt is null
      and rs.topic.deletedAt is null
      and rs.topic.user.id = :userId
      and rs.scheduledDate = :date
      and rs.notificationSent = true
""")
    long countEmailedForUserAndDate(@Param("userId") UUID userId,
                                    @Param("date") LocalDate date);

    @Query("""
    select count(rs) from RevisionSchedule rs
    where rs.deletedAt is null
      and rs.topic.deletedAt is null
      and rs.topic.user.id = :userId
      and rs.scheduledDate = :date
      and rs.notificationSent = true
      and rs.status = :status
""")
    long countEmailedForUserAndDateByStatus(@Param("userId") UUID userId,
                                            @Param("date") LocalDate date,
                                            @Param("status") RevisionStatus status);

    boolean existsByTopicIdAndDayNumberAndDeletedAtIsNull(UUID topicId, int dayNumber);
}