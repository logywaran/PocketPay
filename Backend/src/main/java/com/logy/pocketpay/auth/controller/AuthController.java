package com.logy.pocketpay.auth.controller;

import com.logy.pocketpay.auth.dto.*;
import com.logy.pocketpay.auth.model.LoginResult;
import com.logy.pocketpay.auth.service.AuthCookieService;
import com.logy.pocketpay.auth.service.AuthService;
import com.logy.pocketpay.audit.context.SecurityAuditContext;
import com.logy.pocketpay.auth.exception.InvalidRefreshTokenException;
import com.logy.pocketpay.common.tracing.TraceContext;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final AuthCookieService authCookieService;
    private final TraceContext traceContext;


    @GetMapping("/csrf")
    public ResponseEntity<Void> csrf(CsrfToken csrfToken) {
        csrfToken.getToken();
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest
    ) {

        SecurityAuditContext auditContext = createAuditContext(httpRequest);

        LoginResult result = authService.login(request, auditContext);

        HttpHeaders headers = new HttpHeaders();

        headers.add(
                HttpHeaders.SET_COOKIE,
                authCookieService.createAccessTokenCookie(
                        result.accessToken()
                )
        );

        if (result.refreshToken() != null) {
            headers.add(
                    HttpHeaders.SET_COOKIE,
                    authCookieService.createRefreshTokenCookie(
                            result.refreshToken(),
                            result.refreshTokenExpiresAt(),
                            result.rememberMe()
                    )
            );
        } else {
            headers.add(
                    HttpHeaders.SET_COOKIE,
                    authCookieService.clearRefreshTokenCookie()
            );
        }

        return ResponseEntity.ok()
                .headers(headers)
                .body(new LoginResponse(result.passwordChangeRequired()));
    }

    @PostMapping("/refresh")
    public ResponseEntity<Void> refresh(HttpServletRequest httpRequest) {

        String refreshToken = null;

        Cookie[] cookies = httpRequest.getCookies();

        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if ("refresh_token".equals(cookie.getName())) {
                    refreshToken = cookie.getValue();
                    break;
                }
            }
        }

        if (refreshToken == null || refreshToken.isBlank()) {
            throw new InvalidRefreshTokenException("Invalid or expired refresh token");
        }

        SecurityAuditContext auditContext = createAuditContext(httpRequest);

        Optional<LoginResult> result = authService.refresh(refreshToken, auditContext);

        if (result.isEmpty()) {
            throw new InvalidRefreshTokenException("Invalid or expired refresh token");
        }

        LoginResult loginResult = result.get();

        HttpHeaders headers = new HttpHeaders();

        headers.add(
                HttpHeaders.SET_COOKIE,
                authCookieService.createAccessTokenCookie(
                        loginResult.accessToken()
                )
        );

        headers.add(
                HttpHeaders.SET_COOKIE,
                authCookieService.createRefreshTokenCookie(
                        loginResult.refreshToken(),
                        loginResult.refreshTokenExpiresAt(),
                        loginResult.rememberMe()
                )
        );

        return ResponseEntity.noContent()
                .headers(headers)
                .build();
    }

    @PostMapping("/change-password")
    public ResponseEntity<Void> changePassword(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody ChangePasswordRequest request,
            HttpServletRequest httpRequest
    ) {

        Long userId = Long.valueOf(jwt.getSubject());

        authService.changePassword(
                userId,
                request,
                createAuditContext(httpRequest)
        );

        HttpHeaders headers = new HttpHeaders();

        headers.add(
                HttpHeaders.SET_COOKIE,
                authCookieService.clearAccessTokenCookie()
        );

        headers.add(
                HttpHeaders.SET_COOKIE,
                authCookieService.clearRefreshTokenCookie()
        );

        return ResponseEntity.noContent()
                .headers(headers)
                .build();
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<ForgotPasswordResponse> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequest request,
            HttpServletRequest httpServletRequest
    ) {

        authService.forgotPassword(
                request,
                createAuditContext(httpServletRequest)
        );

        return ResponseEntity.ok(
                new ForgotPasswordResponse(
                        "If an eligible account exists, password reset instructions will be sent to the registered email address."
                )
        );
    }

    @PostMapping("/reset-password")
    public ResponseEntity<Void> resetPassword(
            @Valid @RequestBody ResetPasswordRequest request,
            HttpServletRequest httpServletRequest
    ) {

        authService.resetPassword(
                request,
                createAuditContext(httpServletRequest)
        );

        HttpHeaders headers = new HttpHeaders();

        headers.add(
                HttpHeaders.SET_COOKIE,
                authCookieService.clearAccessTokenCookie().toString()
        );

        headers.add(
                HttpHeaders.SET_COOKIE,
                authCookieService.clearRefreshTokenCookie().toString()
        );

        return ResponseEntity.noContent()
                .headers(headers)
                .build();
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @CookieValue(
                    name = "refresh_token",
                    required = false
            ) String refreshToken,
            HttpServletRequest httpRequest
    ) {

        SecurityAuditContext auditContext = createAuditContext(httpRequest);

        authService.logout(refreshToken, auditContext);

        HttpHeaders headers = new HttpHeaders();

        headers.add(
                HttpHeaders.SET_COOKIE,
                authCookieService.clearAccessTokenCookie()
        );

        headers.add(
                HttpHeaders.SET_COOKIE,
                authCookieService.clearRefreshTokenCookie()
        );

        return ResponseEntity.noContent()
                .headers(headers)
                .build();
    }


    private SecurityAuditContext createAuditContext(
            HttpServletRequest request
    ) {
        return new SecurityAuditContext(
                request.getRemoteAddr(),
                traceContext.getTraceId()
        );
    }

}