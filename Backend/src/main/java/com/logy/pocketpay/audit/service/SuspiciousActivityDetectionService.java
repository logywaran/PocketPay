package com.logy.pocketpay.audit.service;

public interface SuspiciousActivityDetectionService {

    boolean hasRepeatedFailedLogins(Long userId);
}
