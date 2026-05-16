package com.claircore.device.domain.model.commands;

import java.util.UUID;

public record RegisterDeviceCommand(
    String serialNumber,
    String name,
    UUID spaceId
) {
    public RegisterDeviceCommand {
        if (serialNumber == null || serialNumber.isBlank()) {
            throw new IllegalArgumentException("Serial number must not be null or blank");
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Device name must not be null or blank");
        }
        if (spaceId == null) {
            throw new IllegalArgumentException("Space ID must not be null");
        }
    }
}