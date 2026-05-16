package com.claircore.notifications.domain.model.valueobjects;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.util.regex.Pattern;

@Embeddable
public record EmailRecipient(@Column(name = "recipient_email", nullable = false) String address) {
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$");

    public EmailRecipient {
        if (address == null || address.isBlank()) throw new IllegalArgumentException("Recipient email is required");
        if (!EMAIL_PATTERN.matcher(address).matches()) throw new IllegalArgumentException("Recipient email is invalid");
    }
}
