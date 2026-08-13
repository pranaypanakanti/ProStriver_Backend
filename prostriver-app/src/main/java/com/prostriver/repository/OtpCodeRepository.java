package com.prostriver.repository;

import com.prostriver.entity.OtpCode;
import com.prostriver.entity.enums.OtpPurpose;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

public interface OtpCodeRepository extends JpaRepository<OtpCode, UUID> {
    Optional<OtpCode> findTopByEmailAndPurposeOrderByCreatedAtDesc(String email, OtpPurpose purpose);
    long deleteByExpiresAtBefore(LocalDateTime time);
    void deleteAllByEmail(String email);
}