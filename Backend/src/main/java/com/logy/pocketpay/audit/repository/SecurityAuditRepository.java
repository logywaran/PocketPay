package com.logy.pocketpay.audit.repository;

import com.logy.pocketpay.audit.entity.SecurityAuditAction;
import com.logy.pocketpay.audit.entity.SecurityAuditEvent;
import com.logy.pocketpay.audit.entity.SecurityAuditOutcome;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;

public interface SecurityAuditRepository
        extends JpaRepository<SecurityAuditEvent, Long> {

    long countByActionAndOutcomeAndTargetUserIdAndCreatedAtAfter(
            SecurityAuditAction action,
            SecurityAuditOutcome outcome,
            Long targetUserId,
            Instant createdAt
    );
}