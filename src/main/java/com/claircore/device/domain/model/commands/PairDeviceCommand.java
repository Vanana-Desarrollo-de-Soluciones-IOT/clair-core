package com.claircore.device.domain.model.commands;

public record PairDeviceCommand(
    String hardwareId,
    String deviceType
) {
    public PairDeviceCommand {
        if (hardwareId == null || hardwareId.isBlank()) {
            throw new IllegalArgumentException("Hardware ID must not be null or blank");
        }
        if (deviceType == null || deviceType.isBlank()) {
            throw new IllegalArgumentException("Device type must not be null or blank");
        }
    }
}
