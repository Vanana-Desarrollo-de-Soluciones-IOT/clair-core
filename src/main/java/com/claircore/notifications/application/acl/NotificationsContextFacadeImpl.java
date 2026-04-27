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

        EmailLog emailLog = new EmailLog(emailAddress, subject, content, false);

        try {
            emailService.sendEmail(emailAddress, subject, content);
            emailLog = new EmailLog(emailAddress, subject, content, true);
            logger.info("Welcome email sent successfully to {}", emailAddress);
        } catch (Exception e) {
            emailLog.markAsFailed(e.getMessage());
            logger.error("Failed to send welcome email to {}: {}", emailAddress, e.getMessage());
        } finally {
            emailLogRepository.save(emailLog);
        }
    }
}
