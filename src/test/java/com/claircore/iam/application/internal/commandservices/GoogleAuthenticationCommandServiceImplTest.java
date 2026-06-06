package com.claircore.iam.application.internal.commandservices;

import com.claircore.iam.domain.model.commands.AuthenticateWithGoogleCommand;
import com.claircore.iam.domain.model.entities.User;
import com.claircore.iam.domain.model.events.UserAuthenticatedWithGoogleEvent;
import com.claircore.iam.domain.model.events.UserRegisteredEvent;
import com.claircore.iam.domain.model.valueobjects.EmailAddress;
import com.claircore.iam.domain.model.valueobjects.GoogleIdToken;
import com.claircore.iam.domain.model.valueobjects.GoogleUserId;
import com.claircore.iam.domain.model.valueobjects.OAuthProvider;
import com.claircore.iam.domain.model.valueobjects.Password;
import com.claircore.iam.domain.services.GoogleTokenVerifier;
import com.claircore.iam.infrastructure.persistence.jpa.repositories.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GoogleAuthenticationCommandServiceImplTest {

    @Mock
    private GoogleTokenVerifier googleTokenVerifier;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    private GoogleAuthenticationCommandServiceImpl service;

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        service = new GoogleAuthenticationCommandServiceImpl(googleTokenVerifier, userRepository, eventPublisher);
    }

    @Test
    void shouldCreateNewUserAndPublishEventsWhenGoogleIdentityIsNew() {
        when(googleTokenVerifier.verify(any())).thenReturn(Optional.of(
                new com.claircore.iam.domain.model.valueobjects.VerifiedGoogleIdentity(
                        new EmailAddress("user@example.com"),
                        new GoogleUserId("google-subject"),
                        true
                )
        ));
        when(userRepository.findByEmail(new EmailAddress("user@example.com"))).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            ReflectionTestUtils.setField(user, "id", UUID.randomUUID());
            return user;
        });

        Optional<User> result = service.handle(new AuthenticateWithGoogleCommand(new GoogleIdToken("token")));

        assertTrue(result.isPresent());
        assertEquals(OAuthProvider.GOOGLE, result.get().getOauthProvider());
        assertEquals("google-subject", result.get().getOauthSubject());
        verify(userRepository).save(any(User.class));
        verify(eventPublisher).publishEvent(org.mockito.ArgumentMatchers.any(UserRegisteredEvent.class));
        verify(eventPublisher).publishEvent(org.mockito.ArgumentMatchers.any(UserAuthenticatedWithGoogleEvent.class));
    }

    @Test
    void shouldActivateAndLinkExistingMailUserWhenGoogleIdentityMatchesExistingAccount() {
        User existingUser = new User(new EmailAddress("user@example.com"), new Password("encoded-password"));
        ReflectionTestUtils.setField(existingUser, "id", UUID.randomUUID());
        when(googleTokenVerifier.verify(any())).thenReturn(Optional.of(
                new com.claircore.iam.domain.model.valueobjects.VerifiedGoogleIdentity(
                        new EmailAddress("user@example.com"),
                        new GoogleUserId("google-subject"),
                        true
                )
        ));
        when(userRepository.findByEmail(new EmailAddress("user@example.com"))).thenReturn(Optional.of(existingUser));

        Optional<User> result = service.handle(new AuthenticateWithGoogleCommand(new GoogleIdToken("token")));

        assertTrue(result.isPresent());
        assertTrue(result.get().isActive());
        assertEquals(OAuthProvider.GOOGLE, result.get().getOauthProvider());
        assertEquals("google-subject", result.get().getOauthSubject());
        verify(userRepository, never()).save(any());
        verify(eventPublisher).publishEvent(org.mockito.ArgumentMatchers.any(UserAuthenticatedWithGoogleEvent.class));
    }

    @Test
    void shouldReturnEmptyWhenGoogleIdentityCannotBeVerified() {
        when(googleTokenVerifier.verify(any())).thenReturn(Optional.empty());

        Optional<User> result = service.handle(new AuthenticateWithGoogleCommand(new GoogleIdToken("token")));

        assertFalse(result.isPresent());
        verify(userRepository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(org.mockito.ArgumentMatchers.any(Object.class));
    }
}
