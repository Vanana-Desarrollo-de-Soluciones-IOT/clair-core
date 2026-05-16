package com.claircore.notifications.domain.model.commands;

import com.claircore.notifications.domain.model.valueobjects.EmailRecipient;

public record SendVerificationCodeCommand(EmailRecipient recipient, String verificationCode) {
    public SendVerificationCodeCommand {
        if (recipient == null) throw new IllegalArgumentException("Recipient is required");
        if (verificationCode == null || verificationCode.isBlank()) throw new IllegalArgumentException("Verification code is required");
    }
}
