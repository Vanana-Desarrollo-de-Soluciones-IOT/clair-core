package com.claircore.analytics.domain.model.queries;

import com.claircore.analytics.domain.model.valueobjects.DeviceId;
import com.claircore.analytics.domain.model.valueobjects.TrendPeriod;

import java.time.Instant;

public record GetHistoricalTrendQuery(
        DeviceId deviceId,
        TrendPeriod period,
        Instant startDate,
        Instant endDate
) {
    public GetHistoricalTrendQuery {
        if (deviceId == null) {
            throw new IllegalArgumentException("Device ID must not be null");
        }
        if (period == null && (startDate == null || endDate == null)) {
            throw new IllegalArgumentException("Either period or both startDate and endDate must be provided");
        }
    }
}
