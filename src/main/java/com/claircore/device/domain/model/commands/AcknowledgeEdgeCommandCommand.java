package com.claircore.device.domain.model.commands;

import com.claircore.device.domain.model.valueobjects.EdgeCommandResult;

import java.util.UUID;

/**
 * The edge reporting the outcome of a command. {@code hardwareId} is the unit claiming to have run
 * it and is checked against the command's own device before anything else is inspected.
 */
public record AcknowledgeEdgeCommandCommand(
        UUID commandId,
        String hardwareId,
        EdgeCommandResult result,
        String detail
) {
    public AcknowledgeEdgeCommandCommand {
        if (commandId == null) {
            throw new IllegalArgumentException("Command ID must not be null");
        }
        if (hardwareId == null || hardwareId.isBlank()) {
            throw new IllegalArgumentException("Hardware ID must not be null or blank");
        }
        if (result == null) {
            throw new IllegalArgumentException("Result must not be null");
        }
    }
}
