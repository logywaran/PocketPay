package com.logy.pocketpay.user.exception;

import com.logy.pocketpay.common.exception.PocketPayException;

public class PhoneNumberAlreadyRegisteredException extends PocketPayException {

    public PhoneNumberAlreadyRegisteredException(String message) {
        super(message);
    }
}