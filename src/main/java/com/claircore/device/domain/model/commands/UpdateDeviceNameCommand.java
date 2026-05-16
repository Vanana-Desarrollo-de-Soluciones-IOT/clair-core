package com.claircore.device.domain.model.commands;

import java.util.UUID;

public record UpdateDeviceNameCommand(
    UUID deviceId,
    String name
) {
    public UpdateDeviceNameCommand {
        if (deviceId == null) {
            throw new IllegalArgumentException("Device ID must not be null");
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Name must not be null or blank");
        }
    }
}