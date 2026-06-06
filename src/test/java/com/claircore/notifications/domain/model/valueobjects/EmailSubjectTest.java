package com.claircore.notifications.domain.model.valueobjects;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrowsExactly;

class EmailSubjectTest {

    @Test
    void shouldCreateEmailSubjectWhenValueIsValid() {
        var subject = new EmailSubject("Welcome to Clair IOT");

        assertEquals("Welcome to Clair IOT", subject.value());
    }

    @Test
    void shouldRejectBlankSubject() {
        var exception = assertThrowsExactly(IllegalArgumentException.class, () -> new EmailSubject(""));

        assertEquals("Email subject is required", exception.getMessage());
    }

    @Test
    void shouldRejectSubjectLongerThan255Characters() {
        var longSubject = "a".repeat(256);

        var exception = assertThrowsExactly(IllegalArgumentException.class, () -> new EmailSubject(longSubject));

        assertEquals("Email subject cannot exceed 255 characters", exception.getMessage());
    }
}
