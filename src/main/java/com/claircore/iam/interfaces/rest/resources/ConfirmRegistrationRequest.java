package com.claircore.iam.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(description = "Confirm Registration Request")
public record ConfirmRegistrationRequest(
    @Schema(description = "Registration session ID")
    @NotBlank String sessionId,
    @Schema(description = "8-character verification code (e.g., 6G13-789D)")
    @NotBlank @Pattern(regexp = "^[A-Z0-9]{4}-[A-Z0-9]{4}$", message = "Verification code must be in format XXXX-XXXX") String verificationCode
) {}
