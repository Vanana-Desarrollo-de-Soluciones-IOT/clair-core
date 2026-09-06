package com.claircore.iam.application.internal.commandservices;

import com.claircore.iam.domain.model.aggregates.User;
import com.claircore.iam.domain.model.valueobjects.EmailAddress;
import com.claircore.iam.domain.model.valueobjects.OAuthProvider;
import com.claircore.iam.domain.model.valueobjects.Password;
import com.claircore.iam.infrastructure.oauth.google.GoogleAuthorizationCodeTokenClient;
import com.claircore.iam.application.commandservices.GoogleAuthenticationCommandService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GoogleOAuthCallbackApplicationServiceTest {

    @Mock
    private GoogleAuthorizationCodeTokenClient tokenClient;

    @Mock
    private GoogleAuthenticationCommandService authenticationCommandService;

    @InjectMocks
    private GoogleOAuthCallbackApplicationService service;

    @Test
    void shouldReturnEmptyWhenAuthorizationCodeExchangeFails() {
        when(tokenClient.exchangeCodeForIdToken(anyString(), anyString(), anyString(), anyString())).thenReturn(Optional.empty());

        Optional<User> result = service.handle("code", "client-id", "client-secret", "redirect-uri");

        assertTrue(result.isEmpty());
    }

    @Test
    void shouldDelegateAuthenticationWhenAuthorizationCodeExchangesIntoIdToken() {
        when(tokenClient.exchangeCodeForIdToken(anyString(), anyString(), anyString(), anyString())).thenReturn(Optional.of("id-token"));
        User user = new User(new EmailAddress("user@example.com"), OAuthProvider.GOOGLE, "google-subject");
        when(authenticationCommandService.handle(org.mockito.ArgumentMatchers.any(com.claircore.iam.domain.model.commands.AuthenticateWithGoogleCommand.class)))
                .thenReturn(Optional.of(user));

        Optional<User> result = service.handle("code", "client-id", "client-secret", "redirect-uri");

        assertTrue(result.isPresent());
        assertEquals("user@example.com", result.get().getEmail().address());
    }
}
