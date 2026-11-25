package com.logy.pocketpay.auth.service.impl;

import com.logy.pocketpay.audit.context.SecurityAuditContext;
import com.logy.pocketpay.audit.entity.SecurityAuditAction;
import com.logy.pocketpay.audit.entity.SecurityAuditOutcome;
import com.logy.pocketpay.audit.service.SecurityAuditService;
import com.logy.pocketpay.audit.service.SuspiciousActivityDetectionService;
import com.logy.pocketpay.auth.dto.*;
import com.logy.pocketpay.auth.entity.PasswordResetToken;
import com.logy.pocketpay.auth.entity.RefreshTokenRevocationReason;
import com.logy.pocketpay.auth.event.PasswordResetRequestedEvent;
import com.logy.pocketpay.auth.exception.ActiveSessionExistsException;
import com.logy.pocketpay.auth.exception.InvalidCredentialsException;
import com.logy.pocketpay.auth.exception.InvalidPasswordResetTokenException;
import com.logy.pocketpay.auth.exception.PasswordUnchangedException;
import com.logy.pocketpay.auth.model.LoginResult;
import com.logy.pocketpay.auth.model.RefreshTokenResult;
import com.logy.pocketpay.auth.service.AuthService;
import com.logy.pocketpay.auth.service.PasswordResetTokenService;
import com.logy.pocketpay.auth.service.RefreshTokenService;
import com.logy.pocketpay.security.JwtService;
import com.logy.pocketpay.user.entity.User;
import com.logy.pocketpay.user.exception.AccountNotActiveException;
import com.logy.pocketpay.user.repository.UserRepository;
import com.logy.pocketpay.user.entity.UserStatus;
import lombok.extern.slf4j.Slf4j;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Locale;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final SecurityAuditService securityAuditService;
    private final SuspiciousActivityDetectionService suspiciousActivityDetectionService;
    private final PasswordResetTokenService passwordResetTokenService;
    private final ApplicationEventPublisher applicationEventPublisher;


    @Override
    @Transactional
    public LoginResult login(
            LoginRequest request,
            SecurityAuditContext auditContext
    ) {

        String email = request.email().trim().toLowerCase(Locale.ROOT);

        Optional<User> optionalUser = userRepository.findByEmailForUpdate(email);

        if (optionalUser.isEmpty()) {

            securityAuditService.record(
                    SecurityAuditAction.LOGIN_FAILED,
                    SecurityAuditOutcome.FAILURE,
                    null,
                    null,
                    auditContext.ipAddress(),
                    auditContext.traceId(),
                    "Invalid credentials"
            );

            throw new InvalidCredentialsException(
                    "Invalid email or password."
            );
        }

        User user = optionalUser.get();

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {

            securityAuditService.record(
                    SecurityAuditAction.LOGIN_FAILED,
                    SecurityAuditOutcome.FAILURE,
                    null,
                    user.getId(),
                    auditContext.ipAddress(),
                    auditContext.traceId(),
                    "Invalid credentials"
            );

            boolean suspicious =
                    suspiciousActivityDetectionService.hasRepeatedFailedLogins(
                            user.getId()
                    );

            if (suspicious) {
                log.warn(
                        "Repeated failed login attempts detected for userId={}",
                        user.getId()
                );
            }

            throw new InvalidCredentialsException(
                    "Invalid email or password."
            );
        }

        if (user.getStatus() != UserStatus.ACTIVE) {

            securityAuditService.record(
                    SecurityAuditAction.LOGIN_FAILED,
                    SecurityAuditOutcome.DENIED,
                    user.getId(),
                    user.getId(),
                    auditContext.ipAddress(),
                    auditContext.traceId(),
                    "Login denied: account not active"
            );

            throw new AccountNotActiveException(
                    "Your account is not active. Please contact support."
            );
        }

        boolean activeSessionExists =
                refreshTokenService.hasActiveSession(user.getId());

        if (activeSessionExists) {

            if (!request.replaceExistingSession()) {
                throw new ActiveSessionExistsException(
                        "An active session already exists."
                );
            }

            refreshTokenService.revokeAllUserTokens(
                    user.getId(),
                    RefreshTokenRevocationReason.SESSION_REPLACED
            );

            securityAuditService.recordAfterCommit(
                    SecurityAuditAction.SESSION_REPLACED,
                    SecurityAuditOutcome.SUCCESS,
                    user.getId(),
                    user.getId(),
                    auditContext.ipAddress(),
                    auditContext.traceId(),
                    "Existing session replaced by a new login"
            );
        }

        if (user.isMustChangePassword()) {

            String restrictedToken = jwtService.generatePasswordChangeToken(user);

            securityAuditService.recordAfterCommit(
                    SecurityAuditAction.LOGIN_SUCCESS,
                    SecurityAuditOutcome.SUCCESS,
                    user.getId(),
                    user.getId(),
                    auditContext.ipAddress(),
                    auditContext.traceId(),
                    "Credentials verified"
            );

            return new LoginResult(
                    restrictedToken,
                    null,
                    true,
                    null,
                    false
            );
        }

        String accessToken = jwtService.generateAccessToken(user);

        RefreshTokenResult refreshTokenResult =
                refreshTokenService.createRefreshToken(
                        user,
                        request.rememberMe()
                );

        securityAuditService.recordAfterCommit(
                SecurityAuditAction.LOGIN_SUCCESS,
                SecurityAuditOutcome.SUCCESS,
                user.getId(),
                user.getId(),
                auditContext.ipAddress(),
                auditContext.traceId(),
                "Credentials verified"
        );

        return new LoginResult(
                accessToken,
                refreshTokenResult.refreshToken(),
                false,
                refreshTokenResult.expiresAt(),
                refreshTokenResult.rememberMe()
        );
    }

    @Override
    @Transactional
    public Optional<LoginResult> refresh(
            String refreshToken,
            SecurityAuditContext auditContext
    ) {

        return refreshTokenService.rotateRefreshToken(
                refreshToken,
                auditContext
        ).map(result -> {

            String accessToken = jwtService.generateAccessToken(
                    result.user()
            );

            return new LoginResult(
                    accessToken,
                    result.refreshToken(),
                    false,
                    result.expiresAt(),
                    result.rememberMe()
            );
        });
    }

    @Override
    @Transactional
    public void changePassword(
            Long userId,
            ChangePasswordRequest request,
            SecurityAuditContext auditContext
    ) {

        User user = userRepository.findByIdForUpdate(userId)
                .orElseThrow(() -> {

                    securityAuditService.record(
                            SecurityAuditAction.PASSWORD_CHANGE_FAILED,
                            SecurityAuditOutcome.FAILURE,
                            null,
                            null,
                            auditContext.ipAddress(),
                            auditContext.traceId(),
                            "User account not found"
                    );

                    return new InvalidCredentialsException(
                            "Invalid user account."
                    );
                });

        if (user.getStatus() != UserStatus.ACTIVE) {

            securityAuditService.record(
                    SecurityAuditAction.PASSWORD_CHANGE_FAILED,
                    SecurityAuditOutcome.DENIED,
                    user.getId(),
                    user.getId(),
                    auditContext.ipAddress(),
                    auditContext.traceId(),
                    "Account is not active"
            );

            throw new AccountNotActiveException(
                    "Your account is not active. Please contact support."
            );
        }

        if (!passwordEncoder.matches(
                request.currentPassword(),
                user.getPasswordHash()
        )) {

            securityAuditService.record(
                    SecurityAuditAction.PASSWORD_CHANGE_FAILED,
                    SecurityAuditOutcome.FAILURE,
                    user.getId(),
                    user.getId(),
                    auditContext.ipAddress(),
                    auditContext.traceId(),
                    "Incorrect current password"
            );

            throw new InvalidCredentialsException(
                    "Current password is incorrect."
            );
        }

        if (passwordEncoder.matches(
                request.newPassword(),
                user.getPasswordHash()
        )) {
            throw new PasswordUnchangedException(
                    "New password must be different from the current password."
            );
        }

        user.setPasswordHash(
                passwordEncoder.encode(request.newPassword())
        );

        user.setMustChangePassword(false);

        passwordResetTokenService.revokeAllUserTokens(
                user.getId()
        );

        refreshTokenService.revokeAllUserTokens(
                user.getId(),
                RefreshTokenRevocationReason.PASSWORD_CHANGED
        );

        securityAuditService.recordAfterCommit(
                SecurityAuditAction.PASSWORD_CHANGED,
                SecurityAuditOutcome.SUCCESS,
                user.getId(),
                user.getId(),
                auditContext.ipAddress(),
                auditContext.traceId(),
                "Password changed successfully"
        );
    }


    @Override
    @Transactional
    public void forgotPassword(
            ForgotPasswordRequest request,
            SecurityAuditContext auditContext
    ) {

        String email = request.email()
                .trim()
                .toLowerCase(Locale.ROOT);

        Optional<User> optionalUser =
                userRepository.findByEmailForUpdate(email);

        if (optionalUser.isEmpty()) {
            return;
        }

        User user = optionalUser.get();

        if (user.getStatus() != UserStatus.ACTIVE) {
            return;
        }

        String rawToken =
                passwordResetTokenService.createResetToken(user);

        applicationEventPublisher.publishEvent(
                new PasswordResetRequestedEvent(
                        user.getEmail(),
                        rawToken
                )
        );

        securityAuditService.recordAfterCommit(
                SecurityAuditAction.PASSWORD_RESET_REQUESTED,
                SecurityAuditOutcome.SUCCESS,
                null,
                user.getId(),
                auditContext.ipAddress(),
                auditContext.traceId(),
                "Password reset requested"
        );
    }


    @Override
    @Transactional
    public void resetPassword(
            ResetPasswordRequest request,
            SecurityAuditContext auditContext
    ) {

        PasswordResetToken token;

        try {

            token = passwordResetTokenService.validateResetToken(
                    request.token()
            );

        } catch (InvalidPasswordResetTokenException exception) {

            securityAuditService.record(
                    SecurityAuditAction.PASSWORD_RESET_FAILED,
                    SecurityAuditOutcome.FAILURE,
                    null,
                    null,
                    auditContext.ipAddress(),
                    auditContext.traceId(),
                    "Invalid, expired, used or revoked password reset token"
            );

            throw exception;
        }

        User user = token.getUser();

        if (user.getStatus() != UserStatus.ACTIVE) {

            securityAuditService.record(
                    SecurityAuditAction.PASSWORD_RESET_FAILED,
                    SecurityAuditOutcome.DENIED,
                    null,
                    user.getId(),
                    auditContext.ipAddress(),
                    auditContext.traceId(),
                    "Account is not active"
            );

            throw new InvalidPasswordResetTokenException(
                    "Invalid or expired password reset token."
            );
        }

        if (passwordEncoder.matches(
                request.newPassword(),
                user.getPasswordHash()
        )) {
            throw new PasswordUnchangedException(
                    "New password must be different from the current password."
            );
        }

        user.setPasswordHash(
                passwordEncoder.encode(request.newPassword())
        );

        user.setMustChangePassword(false);

        passwordResetTokenService.consumeResetToken(token);

        passwordResetTokenService.revokeAllUserTokens(
                user.getId()
        );

        refreshTokenService.revokeAllUserTokens(
                user.getId(),
                RefreshTokenRevocationReason.PASSWORD_RESET
        );

        securityAuditService.recordAfterCommit(
                SecurityAuditAction.PASSWORD_RESET_COMPLETED,
                SecurityAuditOutcome.SUCCESS,
                user.getId(),
                user.getId(),
                auditContext.ipAddress(),
                auditContext.traceId(),
                "Password reset completed successfully"
        );
    }



    @Override
    @Transactional
    public void logout(
            String refreshToken,
            SecurityAuditContext auditContext
    ) {
        refreshTokenService.revokeForLogout(
                refreshToken,
                auditContext
        );
    }
}