package com.claircore.device.domain.model.commands;

import java.util.UUID;

public record UpdateDeviceSerialNumberCommand(
    UUID deviceId,
    String serialNumber
) {
    public UpdateDeviceSerialNumberCommand {
        if (deviceId == null) {
            throw new IllegalArgumentException("Device ID must not be null");
        }
        if (serialNumber == null || serialNumber.isBlank()) {
            throw new IllegalArgumentException("Serial number must not be null or blank");
        }
    }
}