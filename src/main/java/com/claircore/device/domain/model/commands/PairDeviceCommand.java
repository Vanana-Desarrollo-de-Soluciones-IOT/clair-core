package com.claircore.device.domain.model.commands;

public record PairDeviceCommand(
    String hardwareId
) {
    public PairDeviceCommand {
        if (hardwareId == null || hardwareId.isBlank()) {
            throw new IllegalArgumentException("Hardware ID must not be null or blank");
        }
    }
}
