package com.kronos.repository;

import com.kronos.entity.RevisionSchedule;
import com.kronos.entity.enums.RevisionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

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
}