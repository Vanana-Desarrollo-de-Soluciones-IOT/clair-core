package com.claircore.analytics.domain.model.valueobjects;

import java.time.Instant;

public record KpiDashboardMetrics(
        AirQualityIndex aqi,
        Double averageCo2,
        Double averagePm2_5,
        Double averageTemperature,
        Double averageHumidity,
        MetricTrend co2Trend,
        MetricTrend pm2_5Trend,
        MetricTrend temperatureTrend,
        MetricTrend humidityTrend,
        Instant calculatedAt
) {
    public KpiDashboardMetrics {
        if (aqi == null) {
            throw new IllegalArgumentException("aqi must not be null");
        }
        if (averageCo2 == null) {
            throw new IllegalArgumentException("averageCo2 must not be null");
        }
        if (averagePm2_5 == null) {
            throw new IllegalArgumentException("averagePm2_5 must not be null");
        }
        if (averageTemperature == null) {
            throw new IllegalArgumentException("averageTemperature must not be null");
        }
        if (averageHumidity == null) {
            throw new IllegalArgumentException("averageHumidity must not be null");
        }
        if (calculatedAt == null) {
            throw new IllegalArgumentException("calculatedAt must not be null");
        }
    }
}
