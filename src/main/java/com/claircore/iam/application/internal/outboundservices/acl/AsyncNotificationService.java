package com.claircore.iam.application.internal.outboundservices.acl;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class AsyncNotificationService {

    private final ExternalNotificationService externalNotificationService;

    public AsyncNotificationService(ExternalNotificationService externalNotificationService) {
        this.externalNotificationService = externalNotificationService;
    }

    @Async
    public void sendWelcomeEmail(String email) {
        externalNotificationService.sendWelcomeEmail(email);
    }

    @Async
    public void sendVerificationCode(String email, String code) {
        externalNotificationService.sendVerificationCode(email, code);
    }
}
