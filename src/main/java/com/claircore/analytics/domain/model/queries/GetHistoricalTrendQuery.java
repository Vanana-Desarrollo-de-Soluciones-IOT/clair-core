package com.claircore.analytics.domain.model.queries;

import com.claircore.analytics.domain.model.valueobjects.DeviceId;
import com.claircore.analytics.domain.model.valueobjects.TrendPeriod;

public record GetHistoricalTrendQuery(
        DeviceId deviceId,
        TrendPeriod period
) {
    public GetHistoricalTrendQuery {
        if (deviceId == null) {
            throw new IllegalArgumentException("Device ID must not be null");
        }
        if (period == null) {
            throw new IllegalArgumentException("period must not be null");
        }
    }
}
