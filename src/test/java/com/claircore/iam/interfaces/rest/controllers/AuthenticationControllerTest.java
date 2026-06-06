package com.claircore.iam.interfaces.rest.controllers;

import com.claircore.iam.application.internal.commandservices.GoogleOAuthCallbackApplicationService;
import com.claircore.iam.domain.model.commands.SignOutCommand;
import com.claircore.iam.domain.model.entities.RegistrationSession;
import com.claircore.iam.domain.model.entities.TokenSession;
import com.claircore.iam.domain.model.entities.User;
import com.claircore.iam.domain.model.valueobjects.EmailAddress;
import com.claircore.iam.domain.model.valueobjects.OAuthProvider;
import com.claircore.iam.domain.model.valueobjects.Password;
import com.claircore.iam.domain.model.valueobjects.RegistrationSessionId;
import com.claircore.iam.domain.model.valueobjects.TokenJti;
import com.claircore.iam.domain.model.valueobjects.TokenType;
import com.claircore.iam.domain.model.valueobjects.UserId;
import com.claircore.iam.domain.model.valueobjects.VerificationCode;
import com.claircore.iam.domain.services.GoogleAuthenticationCommandService;
import com.claircore.iam.domain.services.TokenCommandService;
import com.claircore.iam.domain.services.TokenQueryService;
import com.claircore.iam.domain.services.UserCommandService;
import com.claircore.iam.domain.services.UserQueryService;
import com.claircore.iam.infrastructure.oauth.google.GoogleOAuthStateManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;

@WebMvcTest(
        controllers = AuthenticationController.class,
        excludeAutoConfiguration = {SecurityAutoConfiguration.class, UserDetailsServiceAutoConfiguration.class},
        properties = {
                "google.oauth.client-id=test-client-id",
                "google.oauth.client-secret=test-client-secret",
                "google.oauth.redirect-uri=http://localhost/oauth/callback",
                "frontend.url=http://frontend.local"
        }
)
class AuthenticationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserCommandService userCommandService;

    @MockitoBean
    private UserQueryService userQueryService;

    @MockitoBean
    private TokenCommandService tokenCommandService;

    @MockitoBean
    private TokenQueryService tokenQueryService;

    @MockitoBean
    private GoogleAuthenticationCommandService googleAuthenticationCommandService;

    @MockitoBean
    private GoogleOAuthCallbackApplicationService googleOAuthCallbackApplicationService;

    @MockitoBean
    private GoogleOAuthStateManager googleOAuthStateManager;

    @MockitoBean
    private org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

    @Test
    void shouldReturnCreatedWhenSignUpRequestIsValid() throws Exception {
        RegistrationSession session = new RegistrationSession(
                RegistrationSessionId.generate(),
                new EmailAddress("user@example.com"),
                "encoded-password",
                new VerificationCode("6G13-789D"),
                30
        );
        when(userCommandService.handle(any(com.claircore.iam.domain.model.commands.InitiateRegistrationCommand.class)))
                .thenReturn(Optional.of(session));

        mockMvc.perform(post("/api/v1/auth/sign-up")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"user@example.com\",\"password\":\"SecurePass123!\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.sessionId").value(session.sessionId().id()))
                .andExpect(jsonPath("$.message").value("Registration initiated. Please check your email for the verification code."));
    }

    @Test
    void shouldReturnBadRequestWhenSignUpBodyIsInvalid() throws Exception {
        mockMvc.perform(post("/api/v1/auth/sign-up")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturnOkWhenSignInCredentialsAreValid() throws Exception {
        User user = activeUser("user@example.com");
        when(userQueryService.handle(any(com.claircore.iam.domain.model.queries.GetUserByEmailQuery.class)))
                .thenReturn(Optional.of(user));
        when(passwordEncoder.matches("SecurePass123!", "encoded-password")).thenReturn(true);
        when(tokenCommandService.createAccessToken(user)).thenReturn("access-token");
        when(tokenCommandService.createRefreshToken(user)).thenReturn("refresh-token");

        mockMvc.perform(post("/api/v1/auth/sign-in")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"user@example.com\",\"password\":\"SecurePass123!\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("user@example.com"))
                .andExpect(jsonPath("$.token").value("access-token"))
                .andExpect(jsonPath("$.refreshToken").value("refresh-token"));
    }

    @Test
    void shouldReturnUnauthorizedWhenSignInPasswordDoesNotMatch() throws Exception {
        User user = activeUser("user@example.com");
        when(userQueryService.handle(any(com.claircore.iam.domain.model.queries.GetUserByEmailQuery.class)))
                .thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong-password", "encoded-password")).thenReturn(false);

        mockMvc.perform(post("/api/v1/auth/sign-in")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"user@example.com\",\"password\":\"wrong-password\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldReturnNoContentWhenSignOutRequestContainsValidBearerToken() throws Exception {
        UUID userId = UUID.randomUUID();
        when(tokenQueryService.isAccessTokenValid("access-token")).thenReturn(true);
        when(tokenQueryService.getUserIdFromToken("access-token")).thenReturn(Optional.of(userId));

        mockMvc.perform(delete("/api/v1/auth/sign-out")
                        .header("Authorization", "Bearer access-token"))
                .andExpect(status().isNoContent());
    }

    @Test
    void shouldReturnUnauthorizedWhenSignOutHeaderIsMissing() throws Exception {
        mockMvc.perform(delete("/api/v1/auth/sign-out"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldReturnRedirectToFrontendWhenGoogleCallbackSucceeds() throws Exception {
        User user = activeUser("user@example.com");
        when(googleOAuthStateManager.validateState("state")).thenReturn(true);
        when(googleOAuthCallbackApplicationService.handle(anyString(), anyString(), anyString(), anyString()))
                .thenReturn(Optional.of(user));
        when(tokenCommandService.createAccessToken(user)).thenReturn("access-token");
        when(tokenCommandService.createRefreshToken(user)).thenReturn("refresh-token");

        mockMvc.perform(get("/api/v1/auth/google/callback")
                        .param("code", "code")
                        .param("state", "state"))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", containsString("http://frontend.local/auth/callback")))
                .andExpect(header().string("Location", containsString("token=access-token")))
                .andExpect(header().string("Location", containsString("refreshToken=refresh-token")));
    }

    @Test
    void shouldRedirectToErrorWhenGoogleCallbackReturnsNoUser() throws Exception {
        when(googleOAuthStateManager.validateState("state")).thenReturn(true);
        when(googleOAuthCallbackApplicationService.handle(anyString(), anyString(), anyString(), anyString()))
                .thenReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/auth/google/callback")
                        .param("code", "code")
                        .param("state", "state"))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", containsString("http://frontend.local/auth/error?reason=google_oauth_failed")));
    }

    @Test
    void shouldReturnRedirectToErrorWhenGoogleCallbackStateIsInvalid() throws Exception {
        when(googleOAuthStateManager.validateState("bad-state")).thenReturn(false);

        mockMvc.perform(get("/api/v1/auth/google/callback")
                        .param("code", "code")
                        .param("state", "bad-state"))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", containsString("http://frontend.local/auth/error?reason=google_oauth_failed")));
    }

    @Test
    void shouldReturnTokenVerificationPayloadWhenAccessTokenIsValid() throws Exception {
        UUID userId = UUID.randomUUID();
        TokenSession session = new TokenSession(
                new TokenJti(UUID.randomUUID().toString()),
                new EmailAddress("user@example.com"),
                userId,
                TokenType.ACCESS,
                Instant.now(),
                Instant.now().plusSeconds(60)
        );

        when(tokenQueryService.isAccessTokenValid("access-token")).thenReturn(true);
        when(tokenQueryService.getTokenSession("access-token")).thenReturn(Optional.of(session));
        when(tokenQueryService.getUserIdFromToken("access-token")).thenReturn(Optional.of(userId));

        mockMvc.perform(get("/api/v1/auth/verify")
                        .header("Authorization", "Bearer access-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(true))
                .andExpect(jsonPath("$.userId").value(userId.toString()));
    }

    @Test
    void shouldReturnUnauthorizedWhenAccessTokenIsInvalid() throws Exception {
        when(tokenQueryService.isAccessTokenValid("invalid-token")).thenReturn(false);

        mockMvc.perform(get("/api/v1/auth/verify")
                        .header("Authorization", "Bearer invalid-token"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.valid").value(false));
    }

    private User activeUser(String email) {
        User user = new User(new EmailAddress(email), new Password("encoded-password"));
        user.activate();
        ReflectionTestUtils.setField(user, "id", UUID.randomUUID());
        return user;
    }
}
