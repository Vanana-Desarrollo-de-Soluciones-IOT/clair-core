package com.claircore.iam.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;

@Schema(description = "Authenticated User Resource")
public record AuthenticatedUserResource(
    @Schema(description = "User unique identifier")
    UUID id,
    @Schema(description = "User email address")
    String email,
    @Schema(description = "Access token")
    String token,
    @Schema(description = "Refresh token")
    String refreshToken
) {}
