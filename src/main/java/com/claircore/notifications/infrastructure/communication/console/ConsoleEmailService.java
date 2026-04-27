package com.claircore.notifications.infrastructure.communication.console;

import com.claircore.notifications.domain.services.EmailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(name = "notifications.email.provider", havingValue = "console", matchIfMissing = true)
public class ConsoleEmailService implements EmailService {
    private static final Logger logger = LoggerFactory.getLogger(ConsoleEmailService.class);

    @Override
    public void sendEmail(String to, String subject, String content) {
        logger.info("========================================");
        logger.info("MOCK EMAIL SENT (Console Provider)");
        logger.info("To: {}", to);
        logger.info("Subject: {}", subject);
        logger.info("Content: {}", content);
        logger.info("========================================");
    }
}
