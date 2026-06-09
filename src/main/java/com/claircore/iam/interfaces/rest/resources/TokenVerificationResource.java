package com.claircore.iam.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@Schema(description = "Token Verification Response")
public record TokenVerificationResource(
    @Schema(description = "Whether the token is valid")
    boolean valid,
    @Schema(description = "User unique identifier extracted from token")
    UUID userId,
    @Schema(description = "Token expiration timestamp")
    String expiresAt
) {}
