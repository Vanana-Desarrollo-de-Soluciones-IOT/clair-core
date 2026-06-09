package com.claircore.iam.domain.model.commands;

import com.claircore.iam.domain.model.valueobjects.TokenJti;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrowsExactly;

class RotateRefreshTokenCommandTest {

    @Test
    void shouldCreateRotateRefreshTokenCommandWhenJtiIsValid() {
        TokenJti refreshTokenJti = new TokenJti(UUID.randomUUID().toString());

        RotateRefreshTokenCommand command = new RotateRefreshTokenCommand(refreshTokenJti);

        assertEquals(refreshTokenJti, command.refreshTokenJti());
    }

    @Test
    void shouldThrowExceptionWhenRefreshTokenJtiIsMissing() {
        IllegalArgumentException exception = assertThrowsExactly(
                IllegalArgumentException.class,
                () -> new RotateRefreshTokenCommand(null)
        );

        assertEquals("Refresh token JTI is required", exception.getMessage());
    }
}
