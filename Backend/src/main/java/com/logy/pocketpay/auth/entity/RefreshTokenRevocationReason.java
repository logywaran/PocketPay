package com.logy.pocketpay.auth.entity;

public enum RefreshTokenRevocationReason {

    ROTATED,
    LOGOUT,
    PASSWORD_CHANGED,
    ADMIN_REVOKED,
    REUSE_DETECTED,
    SESSION_REPLACED,
    PASSWORD_RESET

}