package com.logy.pocketpay.user.exception;

import com.logy.pocketpay.common.exception.PocketPayException;

public class EmailAlreadyRegisteredException extends PocketPayException {

    public EmailAlreadyRegisteredException(String message) {
        super(message);
    }
}