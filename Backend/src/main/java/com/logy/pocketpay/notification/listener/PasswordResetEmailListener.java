package com.logy.pocketpay.notification.listener;

import com.logy.pocketpay.auth.event.PasswordResetRequestedEvent;
import com.logy.pocketpay.notification.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.web.util.UriComponentsBuilder;

@Slf4j
@Component
@RequiredArgsConstructor
public class PasswordResetEmailListener {

    private final EmailService emailService;

    @Value("${app.frontend.base-url}")
    private String frontendBaseUrl;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handlePasswordResetRequested(
            PasswordResetRequestedEvent event
    ) {

        String resetLink = UriComponentsBuilder
                .fromUriString(frontendBaseUrl)
                .path("/reset-password")
                .queryParam("token", event.rawToken())
                .build()
                .toUriString();

        String subject = "PocketPay - Password Reset Request";

        String body = """
                Hello,

                We received a request to reset your PocketPay password.

                Use the link below to reset your password:

                %s

                This link is valid for 15 minutes and can only be used once.

                If you did not request a password reset, you can safely
                ignore this email.

                Never share this link with anyone.

                Regards,
                PocketPay Security Team
                """.formatted(resetLink);

        try {
            emailService.sendEmail(event.email(), subject, body);

            log.info("Password reset email sent successfully");

        } catch (MailException exception) {

            log.error(
                    "Password reset email delivery failed. exception={}",
                    exception.getClass().getSimpleName()
            );
        }
    }
}
