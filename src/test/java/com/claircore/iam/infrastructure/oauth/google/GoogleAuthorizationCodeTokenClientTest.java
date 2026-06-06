package com.claircore.iam.infrastructure.oauth.google;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GoogleAuthorizationCodeTokenClientTest {

    private RestTemplate restTemplate;
    private GoogleAuthorizationCodeTokenClient client;

    @BeforeEach
    void setUp() {
        restTemplate = mock(RestTemplate.class);
        client = new GoogleAuthorizationCodeTokenClient();
        client.setRestTemplate(restTemplate);
    }

    @Test
    void shouldReturnIdTokenWhenGoogleTokenEndpointRespondsSuccessfully() {
        when(restTemplate.postForEntity(any(String.class), org.mockito.ArgumentMatchers.any(org.springframework.http.HttpEntity.class), org.mockito.ArgumentMatchers.eq(Map.class)))
                .thenReturn(new ResponseEntity<>(Map.of("id_token", "id-token-value"), HttpStatus.OK));

        assertTrue(client.exchangeCodeForIdToken("code", "client-id", "client-secret", "redirect-uri").isPresent());
    }

    @Test
    void shouldReturnEmptyWhenGoogleTokenEndpointFails() {
        when(restTemplate.postForEntity(any(String.class), org.mockito.ArgumentMatchers.any(org.springframework.http.HttpEntity.class), org.mockito.ArgumentMatchers.eq(Map.class)))
                .thenThrow(new RuntimeException("network-down"));

        assertFalse(client.exchangeCodeForIdToken("code", "client-id", "client-secret", "redirect-uri").isPresent());
    }
}
