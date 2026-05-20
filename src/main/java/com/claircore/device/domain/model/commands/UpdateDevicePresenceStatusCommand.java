package com.claircore.device.domain.model.commands;

import com.claircore.device.domain.model.valueobjects.DeviceStatus;
import com.claircore.device.domain.model.valueobjects.HardwareId;

import java.time.Instant;
import java.util.UUID;

public record UpdateDevicePresenceStatusCommand(
        UUID deviceId,
        HardwareId hardwareId,
        DeviceStatus status,
        Instant occurredAt
) {
    public UpdateDevicePresenceStatusCommand {
        if (deviceId == null && hardwareId == null) {
            throw new IllegalArgumentException("deviceId or hardwareId is required");
        }
        if (status == null) {
            throw new IllegalArgumentException("status is required");
        }
        if (status != DeviceStatus.ONLINE
                && status != DeviceStatus.OFFLINE
                && status != DeviceStatus.STANDBY
                && status != DeviceStatus.ERROR) {
            throw new IllegalArgumentException("status must be ONLINE, OFFLINE, STANDBY, or ERROR");
        }
        if (occurredAt == null) {
            occurredAt = Instant.now();
        }
    }
}
