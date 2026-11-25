package com.logy.pocketpay.auth.service.impl;

import com.logy.pocketpay.auth.model.RefreshTokenResult;
import com.logy.pocketpay.auth.entity.RefreshToken;
import com.logy.pocketpay.auth.entity.RefreshTokenRevocationReason;
import com.logy.pocketpay.auth.repository.RefreshTokenRepository;
import com.logy.pocketpay.auth.service.OpaqueTokenService;
import com.logy.pocketpay.auth.service.RefreshTokenService;
import com.logy.pocketpay.audit.context.SecurityAuditContext;
import com.logy.pocketpay.user.entity.User;
import com.logy.pocketpay.user.repository.UserRepository;
import com.logy.pocketpay.user.entity.UserStatus;
import com.logy.pocketpay.audit.entity.SecurityAuditAction;
import com.logy.pocketpay.audit.entity.SecurityAuditOutcome;
import com.logy.pocketpay.audit.service.SecurityAuditService;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RefreshTokenServiceImpl implements RefreshTokenService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;
    private final EntityManager entityManager;
    private final SecurityAuditService securityAuditService;
    private final OpaqueTokenService opaqueTokenService;

    @Override
    @Transactional
    public RefreshTokenResult createRefreshToken(User user, boolean rememberMe) {

        Instant expiresAt = rememberMe
                ? Instant.now().plus(30, ChronoUnit.DAYS)
                : Instant.now().plus(12, ChronoUnit.HOURS);

        String rawToken = createToken(
                user,
                UUID.randomUUID().toString(),
                expiresAt,
                rememberMe
        );

        return new RefreshTokenResult(
                user,
                rawToken,
                expiresAt,
                rememberMe
        );
    }

    @Override
    @Transactional
    public Optional<RefreshTokenResult> rotateRefreshToken(
            String rawToken,
            SecurityAuditContext auditContext
    ) {

        String tokenHash = opaqueTokenService.hashToken(rawToken);

        Optional<RefreshToken> initialLookup =
                refreshTokenRepository.findByTokenHash(tokenHash);

        if (initialLookup.isEmpty()) {
            return Optional.empty();
        }

        Long userId = initialLookup.get().getUser().getId();

        User user = userRepository.findByIdForUpdate(userId)
                .orElseThrow(() -> new IllegalStateException(
                        "Refresh token references a missing user."
                ));

        entityManager.refresh(initialLookup.get());

        RefreshToken currentToken = refreshTokenRepository
                .findByTokenHashForUpdate(tokenHash)
                .orElseThrow(() -> new IllegalStateException(
                        "Refresh token disappeared during rotation."
                ));

        Instant now = Instant.now();


        if (currentToken.getRevokedAt() != null) {

            if (currentToken.getRevocationReason()
                    == RefreshTokenRevocationReason.ROTATED) {

                List<RefreshToken> familyTokens =
                        refreshTokenRepository.findAllByFamilyId(
                                currentToken.getFamilyId()
                        );

                familyTokens.stream()
                        .filter(token -> token.getRevokedAt() == null)
                        .forEach(token -> {
                            token.setRevokedAt(now);
                            token.setRevocationReason(
                                    RefreshTokenRevocationReason.REUSE_DETECTED
                            );
                        });

                refreshTokenRepository.saveAll(familyTokens);

                securityAuditService.recordAfterCommit(
                        SecurityAuditAction.REFRESH_TOKEN_REUSE_DETECTED,
                        SecurityAuditOutcome.DENIED,
                        user.getId(),
                        user.getId(),
                        auditContext.ipAddress(),
                        auditContext.traceId(),
                        "Previously rotated refresh token reused; active token family revoked"
                );
            }

            return Optional.empty();
        }


        if (!currentToken.getExpiresAt().isAfter(now)) {
            return Optional.empty();
        }

        if (user.getStatus() != UserStatus.ACTIVE
                || user.isMustChangePassword()) {
            return Optional.empty();
        }

        currentToken.setRevokedAt(now);
        currentToken.setRevocationReason(
                RefreshTokenRevocationReason.ROTATED
        );

        String newRawToken = createToken(
                user,
                currentToken.getFamilyId(),
                currentToken.getExpiresAt(),
                currentToken.isRememberMe()
        );

        securityAuditService.recordAfterCommit(
                SecurityAuditAction.REFRESH_TOKEN_ROTATED,
                SecurityAuditOutcome.SUCCESS,
                user.getId(),
                user.getId(),
                auditContext.ipAddress(),
                auditContext.traceId(),
                "Refresh token rotated successfully"
        );

        return Optional.of(
                new RefreshTokenResult(
                        user,
                        newRawToken,
                        currentToken.getExpiresAt(),
                        currentToken.isRememberMe()
                )
        );
    }

    @Override
    @Transactional
    public void revokeForLogout(
            String rawToken,
            SecurityAuditContext auditContext
    ) {

        if (rawToken == null || rawToken.isBlank()) {
            return;
        }

        String tokenHash = opaqueTokenService.hashToken(rawToken);

        Optional<RefreshToken> initialLookup =
                refreshTokenRepository.findByTokenHash(tokenHash);

        if (initialLookup.isEmpty()) {
            return;
        }

        Long userId = initialLookup.get().getUser().getId();

        userRepository.findByIdForUpdate(userId)
                .orElseThrow(() -> new IllegalStateException(
                        "Refresh token references a missing user."
                ));

        entityManager.refresh(initialLookup.get());

        RefreshToken currentToken = refreshTokenRepository
                .findByTokenHashForUpdate(tokenHash)
                .orElseThrow(() -> new IllegalStateException(
                        "Refresh token disappeared during logout."
                ));

        if (currentToken.getRevokedAt() != null) {
            return;
        }

        currentToken.setRevokedAt(Instant.now());
        currentToken.setRevocationReason(
                RefreshTokenRevocationReason.LOGOUT
        );

        securityAuditService.recordAfterCommit(
                SecurityAuditAction.LOGOUT,
                SecurityAuditOutcome.SUCCESS,
                userId,
                userId,
                auditContext.ipAddress(),
                auditContext.traceId(),
                "Active refresh token revoked during logout"
        );
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void revokeAllUserTokens(
            Long userId,
            RefreshTokenRevocationReason reason
    ) {
        refreshTokenRepository.revokeAllByUserId(
                userId,
                Instant.now(),
                reason
        );
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasActiveSession(Long userId) {
        return refreshTokenRepository
                .existsByUserIdAndRevokedAtIsNullAndExpiresAtAfter(
                        userId,
                        Instant.now()
                );
    }

    private String createToken(
            User user,
            String familyId,
            Instant expiresAt,
            boolean rememberMe
    ) {

        byte[] randomBytes = new byte[32];
        SECURE_RANDOM.nextBytes(randomBytes);

        String rawToken = opaqueTokenService.generateToken();

        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUser(user);
        refreshToken.setTokenHash(opaqueTokenService.hashToken(rawToken));
        refreshToken.setFamilyId(familyId);
        refreshToken.setExpiresAt(expiresAt);
        refreshToken.setRememberMe(rememberMe);

        refreshTokenRepository.save(refreshToken);

        return rawToken;
    }

}