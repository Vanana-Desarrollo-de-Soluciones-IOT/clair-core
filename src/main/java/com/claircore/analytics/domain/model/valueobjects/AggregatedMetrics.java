package com.claircore.analytics.domain.model.valueobjects;

import java.time.Instant;

public record AggregatedMetrics(
        Integer aqiValue,
        String aqiCategory,
        Double averageCo2,
        Double averagePm2_5,
        Double averageTemperature,
        Double averageHumidity,
        Double co2DeltaPercentage,
        Double pm2_5DeltaPercentage,
        Double temperatureDeltaPercentage,
        Double humidityDeltaPercentage,
        Instant recordedAt,
        Freshness freshness
) {}
