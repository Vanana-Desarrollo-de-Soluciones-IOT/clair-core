package com.claircore.iam.domain.model.commands;

import com.claircore.iam.domain.model.valueobjects.RegistrationSessionId;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrowsExactly;

class ConfirmRegistrationCommandTest {

    @Test
    void shouldCreateConfirmRegistrationCommandWhenValuesAreValid() {
        RegistrationSessionId sessionId = new RegistrationSessionId(UUID.randomUUID().toString());

        ConfirmRegistrationCommand command = new ConfirmRegistrationCommand(sessionId, "6G13-789D");

        assertEquals(sessionId, command.sessionId());
        assertEquals("6G13-789D", command.verificationCode());
    }

    @Test
    void shouldThrowExceptionWhenSessionIdIsMissing() {
        IllegalArgumentException exception = assertThrowsExactly(
                IllegalArgumentException.class,
                () -> new ConfirmRegistrationCommand(null, "6G13-789D")
        );

        assertEquals("Session ID is required", exception.getMessage());
    }

    @Test
    void shouldThrowExceptionWhenVerificationCodeIsBlank() {
        IllegalArgumentException exception = assertThrowsExactly(
                IllegalArgumentException.class,
                () -> new ConfirmRegistrationCommand(new RegistrationSessionId(UUID.randomUUID().toString()), " ")
        );

        assertEquals("Verification code is required", exception.getMessage());
    }
}
