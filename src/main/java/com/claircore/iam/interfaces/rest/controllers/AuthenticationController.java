package com.claircore.iam.interfaces.rest.controllers;

import com.claircore.iam.domain.model.entities.User;
import com.claircore.iam.domain.model.queries.GetUserByEmailQuery;
import com.claircore.iam.domain.model.valueobjects.EmailAddress;
import com.claircore.iam.domain.services.UserCommandService;
import com.claircore.iam.domain.services.UserQueryService;
import com.claircore.iam.infrastructure.tokens.jwt.TokenService;
import com.claircore.iam.interfaces.rest.resources.AuthenticatedUserResource;
import com.claircore.iam.interfaces.rest.resources.SignInRequest;
import com.claircore.iam.interfaces.rest.resources.SignUpRequest;
import com.claircore.iam.interfaces.rest.resources.UserResource;
import com.claircore.iam.interfaces.rest.transform.SignUpCommandFromRequestAssembler;
import com.claircore.iam.interfaces.rest.transform.UserResourceFromEntityAssembler;
import io.swagger.v3.oas.annotations.Operation;
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
@Tag(name = "Authentication", description = "Authentication Endpoints")
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
    @Operation(summary = "Sign up a new user")
    public ResponseEntity<UserResource> signUp(@Valid @RequestBody SignUpRequest request) {
        var signUpCommand = SignUpCommandFromRequestAssembler.toCommandFromRequest(request);
        var user = userCommandService.handle(signUpCommand);
        if (user.isEmpty()) return ResponseEntity.badRequest().build();
        var userResource = UserResourceFromEntityAssembler.toResourceFromEntity(user.get());
        return new ResponseEntity<>(userResource, HttpStatus.CREATED);
    }

    @PostMapping("/sign-in")
    @Operation(summary = "Sign in an existing user")
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
