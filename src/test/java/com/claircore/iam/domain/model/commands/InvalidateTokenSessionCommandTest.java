package com.claircore.iam.domain.model.commands;

import com.claircore.iam.domain.model.valueobjects.TokenJti;
import com.claircore.iam.domain.model.valueobjects.TokenType;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrowsExactly;

class InvalidateTokenSessionCommandTest {

    @Test
    void shouldCreateInvalidateTokenSessionCommandWhenValuesAreValid() {
        TokenJti jti = new TokenJti(UUID.randomUUID().toString());

        InvalidateTokenSessionCommand command = new InvalidateTokenSessionCommand(jti, TokenType.ACCESS);

        assertEquals(jti, command.jti());
        assertEquals(TokenType.ACCESS, command.type());
    }

    @Test
    void shouldThrowExceptionWhenJtiIsMissing() {
        IllegalArgumentException exception = assertThrowsExactly(
                IllegalArgumentException.class,
                () -> new InvalidateTokenSessionCommand(null, TokenType.ACCESS)
        );

        assertEquals("JTI is required", exception.getMessage());
    }

    @Test
    void shouldThrowExceptionWhenTokenTypeIsMissing() {
        TokenJti jti = new TokenJti(UUID.randomUUID().toString());

        IllegalArgumentException exception = assertThrowsExactly(
                IllegalArgumentException.class,
                () -> new InvalidateTokenSessionCommand(jti, null)
        );

        assertEquals("Token type is required", exception.getMessage());
    }
}
