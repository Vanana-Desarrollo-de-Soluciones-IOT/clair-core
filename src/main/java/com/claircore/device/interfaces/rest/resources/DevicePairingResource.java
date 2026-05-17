package com.claircore.device.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@Schema(name = "DevicePairingResource", description = "Response returned when a device pairing flow is started")
public record DevicePairingResource(
        @Schema(description = "Device ID", example = "550e8400-e29b-41d4-a716-446655440000")
        UUID deviceId,
        @Schema(description = "Short-lived claim token shown by the device and used once by the user to claim it", example = "AB45-F3B1")
        String claimToken
) {}
