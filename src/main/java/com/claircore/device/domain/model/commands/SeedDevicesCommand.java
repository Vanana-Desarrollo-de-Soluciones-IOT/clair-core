package com.claircore.device.domain.model.commands;

public record SeedDevicesCommand(int count) {
    public SeedDevicesCommand {
        if (count <= 0) {
            throw new IllegalArgumentException("Count must be positive");
        }
    }
}
