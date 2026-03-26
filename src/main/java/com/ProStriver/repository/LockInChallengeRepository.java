package com.ProStriver.repository;

import com.ProStriver.entity.LockInChallenge;
import com.ProStriver.entity.enums.ChallengeStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LockInChallengeRepository extends JpaRepository<LockInChallenge, UUID> {

    Optional<LockInChallenge> findByUserIdAndStatus(UUID userId, ChallengeStatus status);

    List<LockInChallenge> findAllByUserIdOrderByCreatedAtDesc(UUID userId);

    List<LockInChallenge> findByStatus(ChallengeStatus status);
}