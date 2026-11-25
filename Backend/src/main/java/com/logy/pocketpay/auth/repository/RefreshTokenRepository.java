package com.logy.pocketpay.auth.repository;

import com.logy.pocketpay.auth.entity.RefreshToken;
import com.logy.pocketpay.auth.entity.RefreshTokenRevocationReason;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface RefreshTokenRepository
        extends JpaRepository<RefreshToken, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT token
            FROM RefreshToken token
            WHERE token.tokenHash = :tokenHash
            """)
    Optional<RefreshToken> findByTokenHashForUpdate(
            @Param("tokenHash") String tokenHash
    );

    List<RefreshToken> findAllByFamilyId(String familyId);

    Optional<RefreshToken> findByTokenHash(String tokenHash);

    boolean existsByUserIdAndRevokedAtIsNullAndExpiresAtAfter(
            Long userId,
            Instant now
    );

    @Modifying(flushAutomatically = true)
    @Query("""
        UPDATE RefreshToken token
        SET token.revokedAt = :revokedAt,
            token.revocationReason = :reason
        WHERE token.user.id = :userId
          AND token.revokedAt IS NULL
        """)
    int revokeAllByUserId(
            @Param("userId") Long userId,
            @Param("revokedAt") Instant revokedAt,
            @Param("reason") RefreshTokenRevocationReason reason
    );

}