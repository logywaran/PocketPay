package com.logy.pocketpay.auth.model;

import java.time.Instant;

public record LoginResult(
        String accessToken,
        String refreshToken,
        boolean passwordChangeRequired,
        Instant refreshTokenExpiresAt,
        boolean rememberMe
) {
}