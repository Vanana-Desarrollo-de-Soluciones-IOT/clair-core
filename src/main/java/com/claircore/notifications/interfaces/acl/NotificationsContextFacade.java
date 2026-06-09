package com.claircore.notifications.interfaces.acl;

public interface NotificationsContextFacade {
    void sendWelcomeEmail(String emailAddress);
    void sendVerificationCode(String emailAddress, String code);
}
