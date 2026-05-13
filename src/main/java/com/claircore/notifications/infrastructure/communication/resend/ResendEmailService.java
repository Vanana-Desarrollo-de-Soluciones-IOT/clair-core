package com.claircore.notifications.infrastructure.communication.resend;

import com.claircore.notifications.domain.services.EmailService;
import com.resend.Resend;
import com.resend.core.exception.ResendException;
import com.resend.services.emails.model.CreateEmailOptions;
import com.resend.services.emails.model.CreateEmailResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(name = "notifications.email.provider", havingValue = "resend")
public class ResendEmailService implements EmailService {

    private static final Logger logger = LoggerFactory.getLogger(ResendEmailService.class);

    private final Resend resend;
    private final String fromEmail;

    public ResendEmailService(
            @Value("${resend.api.key}") String apiKey,
            @Value("${resend.from.email}") String fromEmail
    ) {
        this.resend = new Resend(apiKey);
        this.fromEmail = fromEmail;
    }

    @Override
    public void sendEmail(String to, String subject, String content) {
        CreateEmailOptions params = CreateEmailOptions.builder()
                .from(fromEmail)
                .to(to)
                .subject(subject)
                .html(content)
                .build();

        try {
            CreateEmailResponse response = resend.emails().send(params);
            logger.info("Email sent successfully via Resend. ID: {}, To: {}", response.getId(), to);
        } catch (ResendException e) {
            logger.error("Failed to send email through Resend to: {}", to, e);
            throw new RuntimeException("Failed to send email through Resend", e);
        }
    }
}
