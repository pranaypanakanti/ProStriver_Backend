package com.kronos.repository;

import com.kronos.entity.Topic;
import com.kronos.entity.enums.TopicStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

public interface TopicRepository extends JpaRepository<Topic, UUID> {

    @Query("""
            select t from Topic t
            where t.id = :id
              and t.user.id = :userId
              and t.deletedAt is null
            """)
    Optional<Topic> findActiveByIdAndUserId(UUID id, UUID userId);

    @Query("""
            select t from Topic t
            where t.user.id = :userId
              and t.deletedAt is null
              and (:status is null or t.status = :status)
              and (:from is null or t.createdAt >= :from)
              and (:to is null or t.createdAt < :to)
              and (
                   :q is null
                   or lower(t.title) like lower(concat('%', :q, '%'))
                   or lower(t.subject) like lower(concat('%', :q, '%'))
                   or lower(coalesce(t.notes, '')) like lower(concat('%', :q, '%'))
              )
            order by t.createdAt desc
            """)
    Page<Topic> search(UUID userId,
                       TopicStatus status,
                       LocalDateTime from,
                       LocalDateTime to,
                       String q,
                       Pageable pageable);
}