package com.claircore.device.domain.model.queries;

import java.time.Instant;
import java.util.UUID;

public record GetDeviceRosterQuery(Instant since, UUID afterId, int limit) {
    public GetDeviceRosterQuery {
        if (limit < 1 || limit > 200) throw new IllegalArgumentException("limit must be between 1 and 200");
    }
}
