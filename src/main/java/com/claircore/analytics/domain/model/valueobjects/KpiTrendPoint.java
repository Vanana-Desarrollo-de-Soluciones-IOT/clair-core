package com.claircore.analytics.domain.model.valueobjects;

import java.time.Instant;

public record KpiTrendPoint(
        Instant timestamp,
        Double aqiValue,
        Double co2,
        Double pm2_5,
        Double temperature,
        Double humidity
) {
    public KpiTrendPoint {
        if (timestamp == null) {
            throw new IllegalArgumentException("timestamp must not be null");
        }
    }
}
