package com.claircore.iam.infrastructure.tokens.jwt;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JwtTokenEncoderTest {

    private final JwtTokenEncoder encoder = new JwtTokenEncoder("01234567890123456789012345678901");

    @Test
    void shouldGenerateAndExtractClaimsWhenTokenIsValid() {
        UUID userId = UUID.randomUUID();
        String jti = UUID.randomUUID().toString();

        String token = encoder.generateAccessToken(userId, 60000L, jti);

        assertEquals(userId, encoder.extractUserId(token).orElseThrow());
        assertEquals(jti, encoder.extractJti(token).orElseThrow());
        assertEquals("access", encoder.extractType(token).orElseThrow());
    }

    @Test
    void shouldGenerateAndExtractRefreshTokenClaimsWhenTokenIsValid() {
        UUID userId = UUID.randomUUID();
        String jti = UUID.randomUUID().toString();

        String token = encoder.generateRefreshToken(userId, 60000L, jti);

        assertEquals(userId, encoder.extractUserId(token).orElseThrow());
        assertEquals(jti, encoder.extractJti(token).orElseThrow());
        assertEquals("refresh", encoder.extractType(token).orElseThrow());
    }

    @Test
    void shouldReturnEmptyWhenTokenIsMalformed() {
        assertTrue(encoder.extractUserId("not-a-token").isEmpty());
        assertFalse(encoder.extractJti("not-a-token").isPresent());
    }
}
