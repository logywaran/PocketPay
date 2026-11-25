package com.logy.pocketpay.auth.service;

import com.logy.pocketpay.auth.model.RefreshTokenResult;
import com.logy.pocketpay.auth.entity.RefreshTokenRevocationReason;
import com.logy.pocketpay.audit.context.SecurityAuditContext;
import com.logy.pocketpay.user.entity.User;

import java.util.Optional;

public interface RefreshTokenService {

    RefreshTokenResult createRefreshToken(User user, boolean rememberMe);

    Optional<RefreshTokenResult> rotateRefreshToken(
            String rawToken,
            SecurityAuditContext auditContext
    );

    void revokeForLogout(
            String rawToken,
            SecurityAuditContext auditContext
    );

    void revokeAllUserTokens(
            Long userId,
            RefreshTokenRevocationReason reason
    );

    boolean hasActiveSession(Long userId);

}