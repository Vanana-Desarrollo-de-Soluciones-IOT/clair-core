package com.claircore.notifications.domain.services;

import com.claircore.notifications.domain.model.valueobjects.EmailContent;
import com.claircore.notifications.domain.model.valueobjects.EmailRecipient;
import com.claircore.notifications.domain.model.valueobjects.EmailSubject;

public interface EmailDeliveryService {
    void sendEmail(EmailRecipient recipient, EmailSubject subject, EmailContent content);
}
