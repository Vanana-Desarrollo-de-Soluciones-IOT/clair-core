package com.claircore.iam.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Token Verification Response")
public record TokenVerificationResource(
    @Schema(description = "Whether the token is valid")
    boolean valid,
    @Schema(description = "User email extracted from token")
    String email,
    @Schema(description = "Token expiration timestamp")
    String expiresAt
) {}
