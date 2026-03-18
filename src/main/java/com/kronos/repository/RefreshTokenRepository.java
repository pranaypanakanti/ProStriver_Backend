package com.kronos.repository;

import com.kronos.entity.RefreshToken;
import com.kronos.entity.enums.RefreshTokenStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {

    Optional<RefreshToken> findByTokenHash(String tokenHash);

    List<RefreshToken> findAllByUserIdAndStatus(UUID userId, RefreshTokenStatus status);

    long deleteByExpiresAtBefore(LocalDateTime time);
}