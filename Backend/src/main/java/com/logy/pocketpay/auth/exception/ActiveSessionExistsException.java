package com.logy.pocketpay.auth.exception;

public class ActiveSessionExistsException extends RuntimeException {
    public ActiveSessionExistsException(String message) {
        super(message);
    }
}
