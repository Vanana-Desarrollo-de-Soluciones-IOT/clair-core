package com.claircore.alerting.domain.model.events;

import java.time.Instant;
import java.util.UUID;

/**
 * Domain event emitted when an alert is resolved.
 */
public record AlertResolvedEvent(
        UUID alertId,
        UUID deviceId,
        Instant resolvedAt
) {
    public AlertResolvedEvent {
        if (alertId == null) {
            throw new IllegalArgumentException("Alert ID must not be null");
        }
        if (deviceId == null) {
            throw new IllegalArgumentException("Device ID must not be null");
        }
        if (resolvedAt == null) {
            throw new IllegalArgumentException("Resolved at must not be null");
        }
    }
}
