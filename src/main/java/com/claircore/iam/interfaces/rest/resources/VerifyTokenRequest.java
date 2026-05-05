package com.claircore.iam.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Verify Token Request")
public record VerifyTokenRequest(
    @Schema(description = "JWT access token to verify")
    @NotBlank String token
) {}
