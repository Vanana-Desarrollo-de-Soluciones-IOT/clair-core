package com.claircore.device.interfaces.rest.resources;

import com.claircore.device.domain.model.valueobjects.DeviceCommandStatus;
import com.claircore.device.domain.model.valueobjects.DeviceCommandType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;

@Schema(description = "Response representing a device command")
public record DeviceCommandResponse(
        @Schema(description = "Command ID", example = "a1b2c3d4-e5f6-7890-abcd-ef1234567890")
        UUID id,

        @Schema(description = "Target device ID", example = "a1b2c3d4-e5f6-7890-abcd-ef1234567890")
        UUID deviceId,

        @Schema(description = "Command type", example = "STANDBY")
        DeviceCommandType type,

        @Schema(description = "Command delivery/execution status", example = "PENDING")
        DeviceCommandStatus status,

        @Schema(description = "Optional command payload as JSON string", example = "{\"reason\":\"user_requested\"}")
        String payload,

        @Schema(description = "When the command was sent to the edge", example = "2026-05-19T10:15:30Z")
        Instant sentAt,

        @Schema(description = "When the command was executed or failed", example = "2026-05-19T10:15:35Z")
        Instant executedAt,

        @Schema(description = "Failure reason if command failed", example = "Embedded device did not respond")
        String failureReason,

        @Schema(description = "When the command was created", example = "2026-05-19T10:15:00Z")
        Instant createdAt
) {}
