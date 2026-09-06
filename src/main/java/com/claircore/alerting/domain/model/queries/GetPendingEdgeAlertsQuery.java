package com.claircore.alerting.domain.model.queries;

import java.time.Instant;

/** Alerts the edge still has to act on: oldest first, optionally only those after {@code since}. */
public record GetPendingEdgeAlertsQuery(Instant since, int limit) {
    public GetPendingEdgeAlertsQuery {
        if (limit < 1 || limit > 500) {
            throw new IllegalArgumentException("limit must be between 1 and 500");
        }
    }
}
