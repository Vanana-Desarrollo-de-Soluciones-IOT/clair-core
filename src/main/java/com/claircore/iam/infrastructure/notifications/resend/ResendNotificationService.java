package com.claircore.iam.infrastructure.notifications.resend;

import com.claircore.iam.domain.model.valueobjects.EmailAddress;
import com.claircore.iam.domain.services.NotificationService;
import com.resend.Resend;
import com.resend.core.exception.ResendException;
import com.resend.services.emails.model.CreateEmailOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class ResendNotificationService implements NotificationService {

    private final Resend resend;
    private final String fromEmail;

    public ResendNotificationService(
            @Value("${resend.api.key}") String apiKey,
            @Value("${resend.from.email}") String fromEmail
    ) {
        this.resend = new Resend(apiKey);
        this.fromEmail = fromEmail;
    }

    @Override
    public void sendSignUpConfirmation(EmailAddress email) {
        CreateEmailOptions params = CreateEmailOptions.builder()
                .from(fromEmail)
                .to(email.address())
                .subject("Welcome to Clair Core!")
                .html("<strong>Thank you for signing up!</strong>")
                .build();

        try {
            resend.emails().send(params);
        } catch (ResendException e) {
            throw new RuntimeException("Failed to send email via Resend", e);
        }
    }
}
