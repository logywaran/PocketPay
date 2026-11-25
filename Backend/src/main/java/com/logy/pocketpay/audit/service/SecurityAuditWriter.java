package com.logy.pocketpay.audit.service;

import com.logy.pocketpay.audit.entity.SecurityAuditAction;
import com.logy.pocketpay.audit.entity.SecurityAuditEvent;
import com.logy.pocketpay.audit.entity.SecurityAuditOutcome;
import com.logy.pocketpay.audit.repository.SecurityAuditRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SecurityAuditWriter {

    private final SecurityAuditRepository securityAuditRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(
            SecurityAuditAction action,
            SecurityAuditOutcome outcome,
            Long actorUserId,
            Long targetUserId,
            String ipAddress,
            String traceId,
            String details
    ) {

        SecurityAuditEvent event = new SecurityAuditEvent();

        event.setAction(action);
        event.setOutcome(outcome);
        event.setActorUserId(actorUserId);
        event.setTargetUserId(targetUserId);
        event.setIpAddress(ipAddress);
        event.setTraceId(traceId);
        event.setDetails(details);

        securityAuditRepository.save(event);
    }
}