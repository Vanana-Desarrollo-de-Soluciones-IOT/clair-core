package com.claircore.device.domain.model.events;

import java.time.Instant;
import java.util.UUID;

public record DeviceRegisteredEvent(
        UUID deviceId,
        String hardwareId,
        Instant occurredOn
) {
    public DeviceRegisteredEvent {
        if (deviceId == null) {
            throw new IllegalArgumentException("Device ID must not be null");
        }
        if (hardwareId == null || hardwareId.isBlank()) {
            throw new IllegalArgumentException("Hardware ID must not be null or blank");
        }
        if (occurredOn == null) {
            throw new IllegalArgumentException("Occurred on must not be null");
        }
    }
}
