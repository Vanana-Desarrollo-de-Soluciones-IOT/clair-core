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
        aqiValue = aqiValue != null ? Math.round(aqiValue * 100.0) / 100.0 : null;
        co2 = co2 != null ? Math.round(co2 * 100.0) / 100.0 : null;
        pm2_5 = pm2_5 != null ? Math.round(pm2_5 * 100.0) / 100.0 : null;
        temperature = temperature != null ? Math.round(temperature * 100.0) / 100.0 : null;
        humidity = humidity != null ? Math.round(humidity * 100.0) / 100.0 : null;
    }
}
