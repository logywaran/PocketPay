package com.logy.pocketpay.auth.service.impl;

import com.logy.pocketpay.auth.entity.PasswordResetToken;
import com.logy.pocketpay.auth.repository.PasswordResetTokenRepository;
import com.logy.pocketpay.auth.service.OpaqueTokenService;
import com.logy.pocketpay.auth.service.PasswordResetTokenService;
import com.logy.pocketpay.auth.exception.InvalidPasswordResetTokenException;
import com.logy.pocketpay.user.entity.User;
import com.logy.pocketpay.user.repository.UserRepository;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Service
@RequiredArgsConstructor
@Transactional(propagation = Propagation.MANDATORY)
public class PasswordResetTokenServiceImpl
        implements PasswordResetTokenService {

    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final OpaqueTokenService opaqueTokenService;
    private final UserRepository userRepository;
    private final EntityManager entityManager;

    @Override
    public String createResetToken(User user) {

        Instant now = Instant.now();

        passwordResetTokenRepository.revokeAllActiveTokensByUserId(
                user.getId(),
                now
        );

        String rawToken = opaqueTokenService.generateToken();

        PasswordResetToken token = new PasswordResetToken();

        token.setUser(user);
        token.setTokenHash(opaqueTokenService.hashToken(rawToken));
        token.setExpiresAt(now.plus(15, ChronoUnit.MINUTES));

        passwordResetTokenRepository.save(token);

        return rawToken;
    }

    @Override
    public PasswordResetToken validateResetToken(String rawToken) {

        String tokenHash = opaqueTokenService.hashToken(rawToken);

        PasswordResetToken initialToken = passwordResetTokenRepository
                .findByTokenHash(tokenHash)
                .orElseThrow(this::invalidToken);

        Long userId = initialToken.getUser().getId();

        userRepository.findByIdForUpdate(userId)
                .orElseThrow(this::invalidToken);

        PasswordResetToken token = passwordResetTokenRepository
                .findByTokenHashForUpdate(tokenHash)
                .orElseThrow(this::invalidToken);

        entityManager.refresh(token);

        if (token.getUsedAt() != null
                || token.getRevokedAt() != null
                || !token.getExpiresAt().isAfter(Instant.now())) {

            throw invalidToken();
        }

        return token;
    }

    @Override
    public void consumeResetToken(PasswordResetToken token) {

        token.setUsedAt(Instant.now());
    }

    @Override
    public void revokeAllUserTokens(Long userId) {

        passwordResetTokenRepository.revokeAllActiveTokensByUserId(
                userId,
                Instant.now()
        );
    }

    private InvalidPasswordResetTokenException invalidToken() {

        return new InvalidPasswordResetTokenException(
                "Invalid or expired password reset token."
        );
    }
}
