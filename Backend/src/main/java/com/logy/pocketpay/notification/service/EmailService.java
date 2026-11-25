package com.logy.pocketpay.notification.service;

public interface EmailService {

    void sendEmail(
            String recipient,
            String subject,
            String body
    );
}