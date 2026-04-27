package com.claircore.iam.domain.services;

import com.claircore.iam.domain.model.valueobjects.EmailAddress;

public interface NotificationService {
    void sendSignUpConfirmation(EmailAddress email);
}
