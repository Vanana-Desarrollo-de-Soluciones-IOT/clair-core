package com.claircore.device.interfaces.rest.resources;

import com.claircore.device.domain.model.valueobjects.DeviceStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;

import java.time.Instant;
import java.util.UUID;

@Schema(description = "Device presence transition detected by the edge service")
public record DevicePresenceEventRequest(
        @Schema(description = "Core device UUID", example = "f2b1c3d4-aaaa-bbbb-cccc-1234567890ab")
        UUID deviceId,

        @NotBlank
        @Schema(description = "Physical hardware identifier", example = "CLAIR-0001")
        String hardwareId,

        @NotNull
        @Schema(description = "Presence status detected by the edge", example = "ONLINE")
        DeviceStatus status,

        @Schema(description = "When the edge detected the transition", example = "2026-05-19T23:20:05Z")
        Instant occurredAt
) {}
