package com.claircore.alerting.domain.model.queries;

/** Alert transitions after {@code afterSequence} (null: from the start), oldest first. */
public record GetPendingEdgeAlertsQuery(Long afterSequence, int limit) {
    public GetPendingEdgeAlertsQuery {
        if (limit < 1 || limit > 500) {
            throw new IllegalArgumentException("limit must be between 1 and 500");
        }
        if (afterSequence != null && afterSequence < 0) {
            throw new IllegalArgumentException("afterSequence must not be negative");
        }
    }
}
