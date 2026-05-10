package com.claircore.iam.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Google Sign-In Request")
public record GoogleSignInRequest(
    @Schema(description = "Google ID token obtained from the client-side OAuth flow", example = "eyJhbGciOiJSUzI1NiIs...")
    @NotBlank(message = "Google ID token is required")
    String idToken
) {}
