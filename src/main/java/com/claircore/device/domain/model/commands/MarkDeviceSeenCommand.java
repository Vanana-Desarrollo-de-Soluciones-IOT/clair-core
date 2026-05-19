package com.claircore.device.domain.model.commands;

import java.util.UUID;

public record MarkDeviceSeenCommand(UUID deviceId) {
    public MarkDeviceSeenCommand {
        if (deviceId == null) {
            throw new IllegalArgumentException("Device ID must not be null");
        }
    }
}
