package com.logy.pocketpay.auth.model;

import com.logy.pocketpay.user.entity.User;

import java.time.Instant;

public record RefreshTokenResult(
        User user,
        String refreshToken,
        Instant expiresAt,
        boolean rememberMe
) {
}