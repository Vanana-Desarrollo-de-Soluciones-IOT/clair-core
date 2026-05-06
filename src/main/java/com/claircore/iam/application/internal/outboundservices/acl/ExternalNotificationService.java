package com.claircore.iam.application.internal.outboundservices.acl;

import com.claircore.notifications.interfaces.acl.NotificationsContextFacade;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class ExternalNotificationService {

    private static final Logger logger = LoggerFactory.getLogger(ExternalNotificationService.class);

    private final NotificationsContextFacade notificationsContextFacade;

    public ExternalNotificationService(NotificationsContextFacade notificationsContextFacade) {
        this.notificationsContextFacade = notificationsContextFacade;
    }

    @CircuitBreaker(name = "notificationService", fallbackMethod = "sendWelcomeEmailFallback")
    public void sendWelcomeEmail(String email, String userId) {
        notificationsContextFacade.sendWelcomeEmail(email, userId);
    }

    @CircuitBreaker(name = "notificationService", fallbackMethod = "sendVerificationCodeFallback")
    public void sendVerificationCode(String email, String code) {
        notificationsContextFacade.sendVerificationCode(email, code);
    }

    private void sendWelcomeEmailFallback(String email, String userId, Exception ex) {
        logger.error("Circuit breaker active: failed to send welcome email to {}. Reason: {}", email, ex.getMessage());
    }

    private void sendVerificationCodeFallback(String email, String code, Exception ex) {
        logger.error("Circuit breaker active: failed to send verification code to {}. Reason: {}", email, ex.getMessage());
        throw new IllegalStateException("Unable to send verification code at this time. Please try again later.", ex);
    }
}
