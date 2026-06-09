package com.claircore.analytics.domain.model.queries;

import com.claircore.analytics.domain.model.valueobjects.DeviceId;
import java.time.Instant;

public record GetDashboardMetricsQuery(
        DeviceId deviceId,
        String period,
        Instant startDate,
        Instant endDate
) {
    public GetDashboardMetricsQuery {
        if (deviceId == null) {
            throw new IllegalArgumentException("Device ID must not be null");
        }
    }
}
