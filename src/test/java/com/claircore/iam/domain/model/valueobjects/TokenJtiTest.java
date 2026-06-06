package com.claircore.iam.domain.model.valueobjects;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrowsExactly;

class TokenJtiTest {

    @Test
    void shouldCreateTokenJtiWhenUuidIsValid() {
        String jti = UUID.randomUUID().toString();

        TokenJti tokenJti = new TokenJti(jti);

        assertEquals(jti, tokenJti.jti());
    }

    @Test
    void shouldGenerateValidTokenJtiWhenFactoryIsUsed() {
        TokenJti tokenJti = TokenJti.generate();

        assertNotNull(tokenJti.jti());
        assertEquals(UUID.fromString(tokenJti.jti()).toString(), tokenJti.jti());
    }

    @Test
    void shouldThrowExceptionWhenTokenJtiIsBlank() {
        IllegalArgumentException exception = assertThrowsExactly(
                IllegalArgumentException.class,
                () -> new TokenJti(" ")
        );

        assertEquals("Token JTI cannot be null or empty", exception.getMessage());
    }

    @Test
    void shouldThrowExceptionWhenTokenJtiIsNotUuid() {
        IllegalArgumentException exception = assertThrowsExactly(
                IllegalArgumentException.class,
                () -> new TokenJti("not-a-uuid")
        );

        assertEquals("Token JTI must be a valid UUID", exception.getMessage());
    }
}
