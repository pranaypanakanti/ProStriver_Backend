package com.ProStriver.repository;

import com.ProStriver.entity.RefreshToken;
import com.ProStriver.entity.enums.RefreshTokenStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {

    Optional<RefreshToken> findByTokenHash(String tokenHash);

    List<RefreshToken> findAllByUserIdAndStatus(UUID userId, RefreshTokenStatus status);

    long deleteByExpiresAtBefore(LocalDateTime time);

    void deleteByExpiresAtBeforeAndStatus(LocalDateTime time, RefreshTokenStatus status);

    void deleteByRevokedAtBeforeAndStatus(LocalDateTime time, RefreshTokenStatus status);

    // NEW: mark any time-expired ACTIVE tokens as EXPIRED (so cleanup can delete them)
    @Modifying
    @Query("""
        update RefreshToken rt
        set rt.status = com.ProStriver.entity.enums.RefreshTokenStatus.EXPIRED
        where rt.status = com.ProStriver.entity.enums.RefreshTokenStatus.ACTIVE
          and rt.expiresAt < :now
    """)
    int markActiveTokensExpired(LocalDateTime now);
}