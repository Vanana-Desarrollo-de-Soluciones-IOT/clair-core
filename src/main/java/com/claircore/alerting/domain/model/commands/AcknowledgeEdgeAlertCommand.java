package com.claircore.alerting.domain.model.commands;

import java.time.Instant;
import java.util.UUID;

/** An edge device reports that it has shown an alert to a human. */
public record AcknowledgeEdgeAlertCommand(UUID alertId, String hardwareId, Instant acknowledgedAt) {
    public AcknowledgeEdgeAlertCommand {
        if (alertId == null) {
            throw new IllegalArgumentException("Alert ID must not be null");
        }
        if (hardwareId == null || hardwareId.isBlank()) {
            throw new IllegalArgumentException("Hardware ID must not be null or blank");
        }
        if (acknowledgedAt == null) {
            throw new IllegalArgumentException("Acknowledged at must not be null");
        }
    }
}
