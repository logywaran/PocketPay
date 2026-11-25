package com.logy.pocketpay.auth.repository;

import com.logy.pocketpay.auth.entity.PasswordResetToken;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;

public interface PasswordResetTokenRepository
        extends JpaRepository<PasswordResetToken, Long> {

    Optional<PasswordResetToken> findByTokenHash(String tokenHash);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT token FROM PasswordResetToken token
            WHERE token.tokenHash = :tokenHash
            """)
    Optional<PasswordResetToken> findByTokenHashForUpdate(
            @Param("tokenHash") String tokenHash
    );

    @Modifying
    @Query("""
            UPDATE PasswordResetToken token
            SET token.revokedAt = :revokedAt
            WHERE token.user.id = :userId
              AND token.usedAt IS NULL
              AND token.revokedAt IS NULL
              AND token.expiresAt > :revokedAt
            """)
    int revokeAllActiveTokensByUserId(
            @Param("userId") Long userId,
            @Param("revokedAt") Instant revokedAt
    );
}