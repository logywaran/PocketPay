package com.logy.pocketpay.auth.service;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;

@Component
public class AuthCookieService {

    public String createAccessTokenCookie(String token) {

        return ResponseCookie.from("access_token", token)
                .httpOnly(true)
                .secure(true)
                .sameSite("Lax")
                .path("/")
                .maxAge(900)
                .build()
                .toString();
    }

    public String createRefreshTokenCookie(
            String token,
            Instant expiresAt,
            boolean rememberMe
    ) {

        ResponseCookie.ResponseCookieBuilder cookie =
                ResponseCookie.from("refresh_token", token)
                        .httpOnly(true)
                        .secure(true)
                        .sameSite("Lax")
                        .path("/api/v1/auth");

        if (rememberMe) {

            Duration remainingLifetime = Duration.between(
                    Instant.now(),
                    expiresAt
            );

            cookie.maxAge(
                    remainingLifetime.isNegative()
                            ? Duration.ZERO
                            : remainingLifetime
            );
        }

        return cookie.build().toString();
    }

    public String clearAccessTokenCookie() {

        return ResponseCookie.from("access_token", "")
                .httpOnly(true)
                .secure(true)
                .sameSite("Lax")
                .path("/")
                .maxAge(0)
                .build()
                .toString();
    }

    public String clearRefreshTokenCookie() {

        return ResponseCookie.from("refresh_token", "")
                .httpOnly(true)
                .secure(true)
                .sameSite("Lax")
                .path("/api/v1/auth")
                .maxAge(0)
                .build()
                .toString();
    }
}