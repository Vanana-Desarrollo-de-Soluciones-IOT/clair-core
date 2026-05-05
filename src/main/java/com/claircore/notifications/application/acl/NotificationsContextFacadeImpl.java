package com.claircore.notifications.application.acl;

import com.claircore.notifications.domain.model.entities.EmailLog;
import com.claircore.notifications.domain.services.EmailService;
import com.claircore.notifications.infrastructure.persistence.jpa.repositories.EmailLogRepository;
import com.claircore.notifications.interfaces.acl.NotificationsContextFacade;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class NotificationsContextFacadeImpl implements NotificationsContextFacade {

    private static final Logger logger = LoggerFactory.getLogger(NotificationsContextFacadeImpl.class);

    private final EmailService emailService;
    private final EmailLogRepository emailLogRepository;

    public NotificationsContextFacadeImpl(EmailService emailService, EmailLogRepository emailLogRepository) {
        this.emailService = emailService;
        this.emailLogRepository = emailLogRepository;
    }

    @Override
    public void sendWelcomeEmail(String emailAddress, String userId) {
        String subject = "Welcome to Clair IOT Platform!";
        String content = String.format("<h1>Welcome, User %s!</h1><p>We are glad to have you on board.</p>", userId);

        persistEmailLog(emailAddress, subject, true);

        try {
            emailService.sendEmail(emailAddress, subject, content);
            logger.info("Welcome email sent successfully to {}", emailAddress);
        } catch (Exception e) {
            logger.error("Failed to send welcome email to {}: {}", emailAddress, e.getMessage());
        }
    }

    @Override
    public void sendVerificationCode(String emailAddress, String code) {
        String subject = "Your Clair IOT Verification Code";
        String content = String.format("<h1>Verification Code</h1><p>Your verification code is: <strong>%s</strong></p><p>This code will expire in 30 minutes.</p>", code);

        persistEmailLog(emailAddress, subject, false);

        try {
            emailService.sendEmail(emailAddress, subject, content);
            logger.info("Verification code sent successfully to {}", emailAddress);
        } catch (Exception e) {
            logger.error("Failed to send verification code to {}: {}", emailAddress, e.getMessage());
        }
    }

    private void persistEmailLog(String recipientEmail, String subject, boolean sent) {
        try {
            EmailLog emailLog = new EmailLog(recipientEmail, subject, null, sent);
            emailLogRepository.save(emailLog);
        } catch (Exception e) {
            logger.error("Failed to persist email log for {}: {}", recipientEmail, e.getMessage());
        }
    }
}
