package com.claircore.notifications.domain.model.valueobjects;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrowsExactly;

class EmailRecipientTest {

    @Test
    void shouldCreateEmailRecipientWhenValueIsValid() {
        var recipient = new EmailRecipient("user@example.com");

        assertEquals("user@example.com", recipient.address());
    }

    @Test
    void shouldRejectBlankEmailAddress() {
        var exception = assertThrowsExactly(IllegalArgumentException.class, () -> new EmailRecipient(" "));

        assertEquals("Recipient email is required", exception.getMessage());
    }

    @Test
    void shouldRejectInvalidEmailAddress() {
        var exception = assertThrowsExactly(IllegalArgumentException.class, () -> new EmailRecipient("invalid-email"));

        assertEquals("Recipient email is invalid", exception.getMessage());
    }
}
