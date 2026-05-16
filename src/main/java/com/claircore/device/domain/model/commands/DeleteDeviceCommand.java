package com.claircore.device.domain.model.commands;

import java.util.UUID;

public record DeleteDeviceCommand(UUID deviceId) {
    public DeleteDeviceCommand {
        if (deviceId == null) {
            throw new IllegalArgumentException("Device ID must not be null");
        }
    }
}