package com.claircore.iam.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Refresh Token Request")
public record RefreshTokenRequest(
    @Schema(description = "Valid refresh token")
    @NotBlank String refreshToken
) {}
