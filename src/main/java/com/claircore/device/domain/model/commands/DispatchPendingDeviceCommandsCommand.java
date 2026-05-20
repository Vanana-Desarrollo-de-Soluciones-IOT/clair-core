package com.claircore.device.domain.model.commands;

public record DispatchPendingDeviceCommandsCommand(Integer limit) {
    public DispatchPendingDeviceCommandsCommand {
        if (limit != null && limit <= 0) {
            throw new IllegalArgumentException("Limit must be positive");
        }
    }
}
