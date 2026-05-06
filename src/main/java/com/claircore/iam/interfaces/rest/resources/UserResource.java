package com.claircore.iam.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;

@Schema(description = "User Resource")
public record UserResource(
    @Schema(description = "User unique identifier")
    UUID id,
    @Schema(description = "User email address")
    String email
) {}
