package com.logy.pocketpay.audit.service.impl;

import com.logy.pocketpay.audit.service.SecurityAuditWriter;
import com.logy.pocketpay.audit.entity.SecurityAuditAction;
import com.logy.pocketpay.audit.entity.SecurityAuditOutcome;
import com.logy.pocketpay.audit.service.SecurityAuditService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Service
@RequiredArgsConstructor
public class SecurityAuditServiceImpl implements SecurityAuditService {

    private final SecurityAuditWriter securityAuditWriter;

    @Override
    public void record(
            SecurityAuditAction action,
            SecurityAuditOutcome outcome,
            Long actorUserId,
            Long targetUserId,
            String ipAddress,
            String traceId,
            String details
    ) {

        securityAuditWriter.record(
                action,
                outcome,
                actorUserId,
                targetUserId,
                ipAddress,
                traceId,
                details
        );
    }

    @Override
    public void recordAfterCommit(
            SecurityAuditAction action,
            SecurityAuditOutcome outcome,
            Long actorUserId,
            Long targetUserId,
            String ipAddress,
            String traceId,
            String details
    ) {

        if (!TransactionSynchronizationManager.isActualTransactionActive()
                || !TransactionSynchronizationManager.isSynchronizationActive()) {
            throw new IllegalStateException(
                    "An active transaction is required for after-commit auditing"
            );
        }

        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {

                    @Override
                    public void afterCommit() {

                        securityAuditWriter.record(
                                action,
                                outcome,
                                actorUserId,
                                targetUserId,
                                ipAddress,
                                traceId,
                                details
                        );
                    }
                }
        );
    }
}