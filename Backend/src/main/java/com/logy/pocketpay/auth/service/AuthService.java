package com.logy.pocketpay.auth.service;

import com.logy.pocketpay.auth.dto.*;
import com.logy.pocketpay.auth.model.LoginResult;
import com.logy.pocketpay.audit.context.SecurityAuditContext;

import java.util.Optional;

public interface AuthService {

    LoginResult login(
            LoginRequest request,
            SecurityAuditContext auditContext
    );

    Optional<LoginResult> refresh(
            String refreshToken,
            SecurityAuditContext auditContext
    );

    void changePassword(
            Long userId,
            ChangePasswordRequest request,
            SecurityAuditContext auditContext
    );

    void forgotPassword(
            ForgotPasswordRequest request,
            SecurityAuditContext auditContext
    );

    void resetPassword(
            ResetPasswordRequest request,
            SecurityAuditContext auditContext
    );

    void logout(
            String refreshToken,
            SecurityAuditContext auditContext
    );

}