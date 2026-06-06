package com.claircore.notifications.domain.model.valueobjects;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrowsExactly;

class EmailContentTest {

    @Test
    void shouldCreateEmailContentWhenValueIsValid() {
        var content = new EmailContent("<p>Hello</p>");

        assertEquals("<p>Hello</p>", content.html());
    }

    @Test
    void shouldRejectBlankEmailContent() {
        var exception = assertThrowsExactly(IllegalArgumentException.class, () -> new EmailContent(" "));

        assertEquals("Email content is required", exception.getMessage());
    }
}
