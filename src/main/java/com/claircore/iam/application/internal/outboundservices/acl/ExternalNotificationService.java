package com.claircore.iam.application.internal.outboundservices.acl;

import com.claircore.notifications.interfaces.acl.NotificationsContextFacade;
import org.springframework.stereotype.Service;

@Service
public class ExternalNotificationService {

    private final NotificationsContextFacade notificationsContextFacade;

    public ExternalNotificationService(NotificationsContextFacade notificationsContextFacade) {
        this.notificationsContextFacade = notificationsContextFacade;
    }

    public void sendWelcomeEmail(String email, String userId) {
        notificationsContextFacade.sendWelcomeEmail(email, userId);
    }
}
