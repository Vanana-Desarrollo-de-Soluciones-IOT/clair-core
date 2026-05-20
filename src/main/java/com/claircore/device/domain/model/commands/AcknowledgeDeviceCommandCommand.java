package com.claircore.device.domain.model.commands;

import com.claircore.device.domain.model.valueobjects.DeviceCommandStatus;

import java.util.UUID;

public record AcknowledgeDeviceCommandCommand(
        UUID deviceId,
        UUID commandId,
        DeviceCommandStatus status,
        String failureReason
) {
    public AcknowledgeDeviceCommandCommand {
        if (deviceId == null) {
            throw new IllegalArgumentException("Device ID must not be null");
        }
        if (commandId == null) {
            throw new IllegalArgumentException("Command ID must not be null");
        }
        if (status == null) {
            throw new IllegalArgumentException("Device command status must not be null");
        }
        if (status != DeviceCommandStatus.EXECUTED && status != DeviceCommandStatus.FAILED) {
            throw new IllegalArgumentException("ACK status must be EXECUTED or FAILED");
        }
    }
}
