package com.claircore.iam.interfaces.rest.controllers;

import com.claircore.iam.domain.model.entities.User;
import com.claircore.iam.domain.model.queries.GetUserByEmailQuery;
import com.claircore.iam.domain.model.valueobjects.EmailAddress;
import com.claircore.iam.domain.services.UserCommandService;
import com.claircore.iam.domain.services.UserQueryService;
import com.claircore.iam.infrastructure.tokens.jwt.TokenService;
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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = "/api/v1/auth", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Authentication", description = "Authentication and Registration Endpoints")
public class AuthenticationController {

    private final UserCommandService userCommandService;
    private final UserQueryService userQueryService;
    private final TokenService tokenService;
    private final PasswordEncoder passwordEncoder;

    public AuthenticationController(UserCommandService userCommandService, UserQueryService userQueryService, TokenService tokenService, PasswordEncoder passwordEncoder) {
        this.userCommandService = userCommandService;
        this.userQueryService = userQueryService;
        this.tokenService = tokenService;
        this.passwordEncoder = passwordEncoder;
    }

    @PostMapping("/sign-up")
    @RateLimiter(name = "authRateLimiter")
    @Operation(summary = "Sign up a new user")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Registration initiated, verification code sent"),
            @ApiResponse(responseCode = "400", description = "Invalid input or email already exists")
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

        var token = tokenService.generateToken(user.get());
        var authenticatedUserResource = new AuthenticatedUserResource(user.get().getId(), user.get().getEmail().address(), token);
        return ResponseEntity.ok(authenticatedUserResource);
    }
}
