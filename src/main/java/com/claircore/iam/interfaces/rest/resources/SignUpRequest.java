package com.claircore.iam.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Sign Up Request")
public record SignUpRequest(
    @Schema(description = "User email address")
    @NotBlank @Email String email,
    @Schema(description = "User password")
    @NotBlank @Size(min = 8) String password
) {}
