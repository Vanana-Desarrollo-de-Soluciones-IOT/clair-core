package com.claircore.device.domain.model.commands;

import com.claircore.device.domain.model.valueobjects.DeviceCommandType;
import com.claircore.device.domain.model.valueobjects.UserId;

import java.util.UUID;

public record CreateDeviceCommandCommand(
        UUID deviceId,
        DeviceCommandType type,
        String payload,
        UserId userId
) {
    public CreateDeviceCommandCommand {
        if (deviceId == null) {
            throw new IllegalArgumentException("Device ID must not be null");
        }
        if (type == null) {
            throw new IllegalArgumentException("Device command type must not be null");
        }
        if (userId == null) {
            throw new IllegalArgumentException("User ID must not be null");
        }
    }
}
