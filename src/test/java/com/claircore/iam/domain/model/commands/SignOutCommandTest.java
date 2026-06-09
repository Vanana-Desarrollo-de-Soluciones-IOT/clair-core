package com.claircore.iam.domain.model.commands;

import com.claircore.iam.domain.model.valueobjects.UserId;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrowsExactly;

class SignOutCommandTest {

    @Test
    void shouldCreateSignOutCommandWhenUserIdIsValid() {
        UserId userId = new UserId(UUID.randomUUID());

        SignOutCommand command = new SignOutCommand(userId);

        assertEquals(userId, command.userId());
    }

    @Test
    void shouldThrowExceptionWhenUserIdIsMissing() {
        IllegalArgumentException exception = assertThrowsExactly(
                IllegalArgumentException.class,
                () -> new SignOutCommand(null)
        );

        assertEquals("User ID is required", exception.getMessage());
    }
}
