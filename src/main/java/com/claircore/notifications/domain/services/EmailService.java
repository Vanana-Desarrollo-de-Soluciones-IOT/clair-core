package com.claircore.notifications.domain.services;

public interface EmailService {
    void sendEmail(String to, String subject, String content);
}
