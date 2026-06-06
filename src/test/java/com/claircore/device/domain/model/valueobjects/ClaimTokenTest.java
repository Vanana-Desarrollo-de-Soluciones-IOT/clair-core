package com.claircore.device.domain.model.valueobjects;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrowsExactly;

class ClaimTokenTest {

    @Test
    void shouldCreateClaimTokenWhenUsingShortCodeFormat() {
        ClaimToken claimToken = new ClaimToken("AB45-F3B1");

        assertEquals("AB45-F3B1", claimToken.value());
    }

    @Test
    void shouldCreateClaimTokenWhenUsingLegacyBase64UrlFormat() {
        ClaimToken claimToken = new ClaimToken("legacy_claim_token_123");

        assertEquals("legacy_claim_token_123", claimToken.value());
    }

    @Test
    void shouldThrowExceptionWhenClaimTokenIsBlank() {
        IllegalArgumentException exception = assertThrowsExactly(
                IllegalArgumentException.class,
                () -> new ClaimToken("")
        );

        assertEquals("Claim token must not be null or blank", exception.getMessage());
    }

    @Test
    void shouldThrowExceptionWhenClaimTokenHasInvalidFormat() {
        IllegalArgumentException exception = assertThrowsExactly(
                IllegalArgumentException.class,
                () -> new ClaimToken("invalid-token")
        );

        assertEquals("Claim token must match AB45-F3B1", exception.getMessage());
    }
}
