package com.claircore.device.domain.model.commands;

import com.claircore.device.domain.model.valueobjects.DeviceStatus;

import java.util.UUID;

public record UpdateDeviceStatusCommand(
    UUID deviceId,
    DeviceStatus status
) {
    public UpdateDeviceStatusCommand {
        if (deviceId == null) {
            throw new IllegalArgumentException("Device ID must not be null");
        }
        if (status == null) {
            throw new IllegalArgumentException("Status must not be null");
        }
    }
}