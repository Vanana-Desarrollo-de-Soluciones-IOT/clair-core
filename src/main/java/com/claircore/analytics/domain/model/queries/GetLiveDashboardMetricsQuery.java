package com.claircore.analytics.domain.model.queries;

import com.claircore.analytics.domain.model.valueobjects.DeviceId;

public record GetLiveDashboardMetricsQuery(
        DeviceId deviceId
) {
    public GetLiveDashboardMetricsQuery {
        if (deviceId == null) {
            throw new IllegalArgumentException("Device ID must not be null");
        }
    }
}
