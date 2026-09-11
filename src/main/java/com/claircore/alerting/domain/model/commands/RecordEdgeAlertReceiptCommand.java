package com.claircore.alerting.domain.model.commands;

import java.util.UUID;

/** The edge confirms it durably stored transition {@code sequence} of an alert. Not a business ACK. */
public record RecordEdgeAlertReceiptCommand(UUID alertId, String hardwareId, long sequence) {
    public RecordEdgeAlertReceiptCommand {
        if (alertId == null) {
            throw new IllegalArgumentException("Alert ID must not be null");
        }
        if (hardwareId == null || hardwareId.isBlank()) {
            throw new IllegalArgumentException("Hardware ID must not be null or blank");
        }
        if (sequence < 0) {
            throw new IllegalArgumentException("Sequence must not be negative");
        }
    }
}
