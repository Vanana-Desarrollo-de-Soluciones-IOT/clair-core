package com.claircore.iam.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

@Schema(description = "Confirm Registration Request")
public record ConfirmRegistrationRequest(
    @Schema(description = "Registration session ID", example = "550e8400-e29b-41d4-a716-446655440000")
    @NotBlank String sessionId,
    @Schema(description = "8-character verification code", example = "6G13-789D")
    @NotBlank @Pattern(regexp = "^[A-Z0-9]{4}-[A-Z0-9]{4}$", message = "Verification code must be in format XXXX-XXXX") String verificationCode
) {}
