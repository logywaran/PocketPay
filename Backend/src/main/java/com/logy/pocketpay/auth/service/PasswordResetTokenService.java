package com.logy.pocketpay.auth.service;

import com.logy.pocketpay.auth.entity.PasswordResetToken;
import com.logy.pocketpay.user.entity.User;

public interface PasswordResetTokenService {

    String createResetToken(User user);

    PasswordResetToken validateResetToken(String rawToken);

    void consumeResetToken(PasswordResetToken token);

    void revokeAllUserTokens(Long userId);
}