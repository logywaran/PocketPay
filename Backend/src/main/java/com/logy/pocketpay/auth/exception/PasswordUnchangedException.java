package com.logy.pocketpay.auth.exception;

public class PasswordUnchangedException extends RuntimeException {
    public PasswordUnchangedException(String message) {
        super(message);
    }
}
