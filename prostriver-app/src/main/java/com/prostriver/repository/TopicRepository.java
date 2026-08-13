package com.prostriver.repository;

import com.prostriver.entity.Topic;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TopicRepository extends JpaRepository<Topic, UUID>, JpaSpecificationExecutor<Topic> {

    @Query("""
            select t from Topic t
            where t.id = :id
              and t.user.id = :userId
              and t.deletedAt is null
            """)
    Optional<Topic> findActiveByIdAndUserId(UUID id, UUID userId);

    @Query("""
    select count(t) from Topic t
    where t.deletedAt is null
      and t.user.id = :userId
      and t.createdAt >= :from
      and t.createdAt < :to
""")
    long countCreatedByUserIdAndRange(@Param("userId") UUID userId,
                                      @Param("from") LocalDateTime from,
                                      @Param("to") LocalDateTime to);

    @Query("""
        select t from Topic t
        where t.user.id = :userId
          and t.deletedAt is null
          and t.createdAt >= :since
        order by t.createdAt desc
    """)
    List<Topic> findAllByUserIdAndCreatedSince(@Param("userId") UUID userId,
                                               @Param("since") LocalDateTime since);
}