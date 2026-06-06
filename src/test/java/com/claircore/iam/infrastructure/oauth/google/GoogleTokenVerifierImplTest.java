package com.claircore.iam.infrastructure.oauth.google;

import com.claircore.iam.domain.model.valueobjects.EmailAddress;
import com.claircore.iam.domain.model.valueobjects.GoogleIdToken;
import com.claircore.iam.domain.model.valueobjects.VerifiedGoogleIdentity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GoogleTokenVerifierImplTest {

    private RestTemplate restTemplate;
    private GoogleTokenVerifierImpl verifier;

    @BeforeEach
    void setUp() {
        restTemplate = mock(RestTemplate.class);
        verifier = new GoogleTokenVerifierImpl("primary-client-id", "");
        ReflectionTestUtils.setField(verifier, "restTemplate", restTemplate);
    }

    @Test
    void shouldReturnVerifiedIdentityWhenGooglePayloadIsValid() {
        Map<String, Object> payload = new HashMap<>();
        payload.put("iss", "https://accounts.google.com");
        payload.put("aud", "primary-client-id");
        payload.put("email_verified", "true");
        payload.put("exp", String.valueOf(System.currentTimeMillis() / 1000 + 600));
        payload.put("email", "user@example.com");
        payload.put("sub", "google-subject");
        when(restTemplate.getForObject(anyString(), org.mockito.ArgumentMatchers.eq(Map.class))).thenReturn(payload);

        java.util.Optional<VerifiedGoogleIdentity> result = verifier.verify(new GoogleIdToken("id-token"));

        assertTrue(result.isPresent());
        assertEquals("user@example.com", result.get().email().address());
        assertEquals("google-subject", result.get().userId().subject());
    }

    @Test
    void shouldReturnEmptyWhenAudienceDoesNotMatchAllowedClientIds() {
        Map<String, Object> payload = new HashMap<>();
        payload.put("iss", "https://accounts.google.com");
        payload.put("aud", "other-client-id");
        payload.put("email_verified", "true");
        payload.put("exp", String.valueOf(System.currentTimeMillis() / 1000 + 600));
        payload.put("email", "user@example.com");
        payload.put("sub", "google-subject");
        when(restTemplate.getForObject(anyString(), org.mockito.ArgumentMatchers.eq(Map.class))).thenReturn(payload);

        assertFalse(verifier.verify(new GoogleIdToken("id-token")).isPresent());
    }
}
