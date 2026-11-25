package com.logy.pocketpay.common.exception;

public abstract class PocketPayException extends RuntimeException {

    protected PocketPayException(String message) {
        super(message);
    }

    protected PocketPayException(String message, Throwable cause) {
        super(message, cause);
    }
}