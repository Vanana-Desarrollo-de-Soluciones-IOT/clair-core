package com.claircore.device.interfaces.rest.resources;

import com.claircore.device.domain.model.valueobjects.DeviceStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;

@Schema(description = "Current device status snapshot for UX polling")
public record DeviceStatusResponse(
        @Schema(description = "Device ID", example = "a1b2c3d4-e5f6-7890-abcd-ef1234567890")
        UUID deviceId,

        @Schema(description = "Current presence/status", example = "ONLINE")
        DeviceStatus status,

        @Schema(description = "When the device was last seen online/standby/error", example = "2026-05-19T10:15:30Z")
        Instant lastSeenAt
) {}

