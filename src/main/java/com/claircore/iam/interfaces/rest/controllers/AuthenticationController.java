package com.claircore.iam.interfaces.rest.controllers;

import com.claircore.iam.domain.model.queries.GetUserByEmailQuery;
import com.claircore.iam.domain.model.valueobjects.EmailAddress;
import com.claircore.iam.domain.services.TokenCommandService;
import com.claircore.iam.domain.services.TokenQueryService;
import com.claircore.iam.domain.services.UserCommandService;
import com.claircore.iam.domain.services.UserQueryService;
import com.claircore.iam.interfaces.rest.resources.*;
import com.claircore.iam.interfaces.rest.transform.*;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = "/api/v1/auth", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Authentication", description = "Authentication and Registration Endpoints")
public class AuthenticationController {

    private final UserCommandService userCommandService;
    private final UserQueryService userQueryService;
    private final TokenCommandService tokenCommandService;
    private final TokenQueryService tokenQueryService;
    private final PasswordEncoder passwordEncoder;

    public AuthenticationController(UserCommandService userCommandService, UserQueryService userQueryService,
                                    TokenCommandService tokenCommandService, TokenQueryService tokenQueryService,
                                    PasswordEncoder passwordEncoder) {
        this.userCommandService = userCommandService;
        this.userQueryService = userQueryService;
        this.tokenCommandService = tokenCommandService;
        this.tokenQueryService = tokenQueryService;
        this.passwordEncoder = passwordEncoder;
    }

    @PostMapping("/sign-up")
    @RateLimiter(name = "authRateLimiter")
    @Operation(summary = "Sign up a new user")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Registration initiated, verification code sent"),
            @ApiResponse(responseCode = "400", description = "Invalid input")
    })
    public ResponseEntity<RegistrationInitiatedResource> signUp(@Valid @RequestBody InitiateRegistrationRequest request) {
        var command = InitiateRegistrationCommandFromRequestAssembler.toCommandFromRequest(request);
        var session = userCommandService.handle(command);
        if (session.isEmpty()) return ResponseEntity.badRequest().build();
        var resource = RegistrationInitiatedResourceFromSessionAssembler.toResourceFromSession(session.get());
        return new ResponseEntity<>(resource, HttpStatus.CREATED);
    }

    @PostMapping("/confirm")
    @RateLimiter(name = "authRateLimiter")
    @Operation(summary = "Confirm registration with verification code")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Registration confirmed, user created"),
            @ApiResponse(responseCode = "400", description = "Invalid or expired session/code")
    })
    public ResponseEntity<UserResource> confirm(@Valid @RequestBody ConfirmRegistrationRequest request) {
        var command = ConfirmRegistrationCommandFromRequestAssembler.toCommandFromRequest(request);
        var user = userCommandService.handle(command);
        if (user.isEmpty()) return ResponseEntity.badRequest().build();
        var resource = UserResourceFromEntityAssembler.toResourceFromEntity(user.get());
        return new ResponseEntity<>(resource, HttpStatus.CREATED);
    }

    @PostMapping("/sign-in")
    @RateLimiter(name = "authRateLimiter")
    @Operation(summary = "Sign in an existing verified user")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Authentication successful"),
            @ApiResponse(responseCode = "401", description = "Invalid credentials or user not verified")
    })
    public ResponseEntity<AuthenticatedUserResource> signIn(@Valid @RequestBody SignInRequest request) {
        var getUserByEmailQuery = new GetUserByEmailQuery(new EmailAddress(request.email()));
        var user = userQueryService.handle(getUserByEmailQuery);

        if (user.isEmpty() || !passwordEncoder.matches(request.password(), user.get().getPassword().passwordHash())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        var token = tokenCommandService.createAccessToken(user.get());
        var refreshToken = tokenCommandService.createRefreshToken(user.get());
        var authenticatedUserResource = new AuthenticatedUserResource(user.get().getId(), user.get().getEmail().address(), token, refreshToken);
        return ResponseEntity.ok(authenticatedUserResource);
    }

    @PostMapping("/refresh")
    @RateLimiter(name = "authRateLimiter")
    @Operation(summary = "Refresh access token using a valid refresh token")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "New access token generated"),
            @ApiResponse(responseCode = "401", description = "Invalid or expired refresh token")
    })
    public ResponseEntity<AuthenticatedUserResource> refreshToken(@Valid @RequestBody RefreshTokenRequest request) {
        if (!tokenQueryService.isRefreshTokenValid(request.refreshToken())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        var email = tokenQueryService.getEmailFromToken(request.refreshToken());
        var user = userQueryService.handle(new GetUserByEmailQuery(new EmailAddress(email.orElse(""))));

        if (user.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        var newRefreshToken = tokenCommandService.rotateRefreshToken(request.refreshToken())
                .orElse(null);
        if (newRefreshToken == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        var newToken = tokenCommandService.createAccessToken(user.get());
        var resource = new AuthenticatedUserResource(user.get().getId(), user.get().getEmail().address(), newToken, newRefreshToken);
        return ResponseEntity.ok(resource);
    }

    @GetMapping("/verify")
    @RateLimiter(name = "authRateLimiter")
    @Operation(summary = "Verify if an access token is valid and return its metadata")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Token is valid"),
            @ApiResponse(responseCode = "401", description = "Invalid or expired token")
    })
    public ResponseEntity<TokenVerificationResource> verifyToken(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new TokenVerificationResource(false, null, null));
        }

        String token = authHeader.substring(7);

        if (!tokenQueryService.isAccessTokenValid(token)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new TokenVerificationResource(false, null, null));
        }

        var email = tokenQueryService.getEmailFromToken(token);
        var session = tokenQueryService.getTokenSession(token);
        var expiresAt = session.map(s -> s.expiresAt().toString()).orElse(null);
        return ResponseEntity.ok(new TokenVerificationResource(true, email.orElse(null), expiresAt));
    }
}
