package com.logy.pocketpay.audit.context;

public record SecurityAuditContext(
        String ipAddress,
        String traceId
) {
}