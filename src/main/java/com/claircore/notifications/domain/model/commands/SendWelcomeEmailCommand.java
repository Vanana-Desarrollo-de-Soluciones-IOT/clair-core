package com.claircore.notifications.domain.model.commands;

import com.claircore.notifications.domain.model.valueobjects.EmailRecipient;

public record SendWelcomeEmailCommand(EmailRecipient recipient) {
    public SendWelcomeEmailCommand {
        if (recipient == null) throw new IllegalArgumentException("Recipient is required");
    }
}
