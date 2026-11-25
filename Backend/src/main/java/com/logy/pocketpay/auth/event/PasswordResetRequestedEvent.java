package com.logy.pocketpay.auth.event;

public record PasswordResetRequestedEvent(
        String email,
        String rawToken
) {

    @Override
    public String toString() {
        return "PasswordResetRequestedEvent[REDACTED]";
    }
}