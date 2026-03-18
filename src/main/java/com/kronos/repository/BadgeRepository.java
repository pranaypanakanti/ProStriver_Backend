package com.kronos.repository;

import com.kronos.entity.Badge;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface BadgeRepository extends JpaRepository<Badge, UUID> {

    Optional<Badge> findByLockInChallengeId(UUID lockInChallengeId);

    boolean existsByLockInChallengeId(UUID lockInChallengeId);
}