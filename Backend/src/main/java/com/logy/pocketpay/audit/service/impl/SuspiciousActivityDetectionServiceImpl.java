package com.logy.pocketpay.audit.service.impl;

import com.logy.pocketpay.audit.service.SuspiciousActivityDetectionService;
import com.logy.pocketpay.audit.entity.SecurityAuditAction;
import com.logy.pocketpay.audit.entity.SecurityAuditOutcome;
import com.logy.pocketpay.audit.repository.SecurityAuditRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Service
@RequiredArgsConstructor
public class SuspiciousActivityDetectionServiceImpl
        implements SuspiciousActivityDetectionService {

    private static final int FAILED_LOGIN_THRESHOLD = 5;
    private static final int FAILED_LOGIN_WINDOW_MINUTES = 10;

    private final SecurityAuditRepository securityAuditRepository;

    @Override
    public boolean hasRepeatedFailedLogins(Long userId) {

        if (userId == null) {
            return false;
        }

        Instant windowStart = Instant.now()
                .minus(FAILED_LOGIN_WINDOW_MINUTES, ChronoUnit.MINUTES);

        long failedAttempts =
                securityAuditRepository
                        .countByActionAndOutcomeAndTargetUserIdAndCreatedAtAfter(
                                SecurityAuditAction.LOGIN_FAILED,
                                SecurityAuditOutcome.FAILURE,
                                userId,
                                windowStart
                        );

        return failedAttempts >= FAILED_LOGIN_THRESHOLD;
    }
}