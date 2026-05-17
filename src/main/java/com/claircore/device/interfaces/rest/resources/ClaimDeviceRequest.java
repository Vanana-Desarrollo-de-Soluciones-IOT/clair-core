package com.claircore.device.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record ClaimDeviceRequest(
    @NotBlank
    @Schema(description = "One-time claim token printed, shown, or provisioned with the physical sensor", example = "mP0oYThqLwiRV3xOjCb77A")
    String claimToken,

    @NotNull
    @Schema(description = "Target space owned by the authenticated user", example = "f013dfad-2a2d-4f62-862e-e29d36842228")
    UUID spaceId
) {}
