package com.claircore.device.domain.model.queries;

import java.time.Instant;

/**
 * The edge asking for work. A null {@code hardwareId} means every unit; a null {@code since} means
 * no lower bound, which is what the edge actually sends on every poll.
 */
public record ClaimPendingEdgeCommandsQuery(String hardwareId, Instant since, int limit) {
    public ClaimPendingEdgeCommandsQuery {
        if (limit < 1 || limit > 500) {
            throw new IllegalArgumentException("limit must be between 1 and 500");
        }
    }
}
