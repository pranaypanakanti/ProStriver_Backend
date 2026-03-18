package com.kronos.repository;

import com.kronos.entity.Topic;
import com.kronos.entity.enums.TopicStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface TopicRepository extends JpaRepository<Topic, UUID> {

    List<Topic> findAllByUserIdOrderByCreatedAtDesc(UUID userId);

    List<Topic> findAllByUserIdAndStatusOrderByCreatedAtDesc(UUID userId, TopicStatus status);

    List<Topic> findAllByUserIdAndSubjectIgnoreCaseOrderByCreatedAtDesc(UUID userId, String subject);

    List<Topic> findAllByUserIdAndSubjectIgnoreCaseAndStatusOrderByCreatedAtDesc(
            UUID userId,
            String subject,
            TopicStatus status
    );

    List<Topic> findAllByUserIdAndTitleContainingIgnoreCaseOrderByCreatedAtDesc(UUID userId, String title);
}