package com.claircore.device.domain.model.commands;

import java.util.Map;
import java.util.UUID;

public record UpdateDeviceConfigurationCommand(
    UUID deviceId,
    Map<String, String> configuration
) {
    public UpdateDeviceConfigurationCommand {
        if (deviceId == null) {
            throw new IllegalArgumentException("Device ID must not be null");
        }
        if (configuration == null) {
            throw new IllegalArgumentException("Configuration must not be null");
        }
    }
}