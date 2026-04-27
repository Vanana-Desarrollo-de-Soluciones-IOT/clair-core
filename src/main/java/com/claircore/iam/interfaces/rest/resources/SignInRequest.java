package com.claircore.iam.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Sign In Request")
public record SignInRequest(
    @Schema(description = "User email address")
    @NotBlank @Email String email,
    @Schema(description = "User password")
    @NotBlank String password
) {}
