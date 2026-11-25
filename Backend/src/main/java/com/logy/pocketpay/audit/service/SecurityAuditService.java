package com.logy.pocketpay.audit.service;

import com.logy.pocketpay.audit.entity.SecurityAuditAction;
import com.logy.pocketpay.audit.entity.SecurityAuditOutcome;

public interface SecurityAuditService {

    void record(
            SecurityAuditAction action,
            SecurityAuditOutcome outcome,
            Long actorUserId,
            Long targetUserId,
            String ipAddress,
            String traceId,
            String details
    );

    void recordAfterCommit(
            SecurityAuditAction action,
            SecurityAuditOutcome outcome,
            Long actorUserId,
            Long targetUserId,
            String ipAddress,
            String traceId,
            String details
    );
}