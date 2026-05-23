package com.claircore.analytics.interfaces.rest.transform;

import com.claircore.analytics.domain.model.valueobjects.KpiDashboardMetrics;
import com.claircore.analytics.domain.model.valueobjects.KpiTrendPoint;
import com.claircore.analytics.interfaces.rest.resources.DashboardMetricsResponse;
import com.claircore.analytics.interfaces.rest.resources.TrendChartResponse;

import java.util.List;

public class AnalyticsTransform {

    private AnalyticsTransform() {}

    public static DashboardMetricsResponse toDashboardResponse(KpiDashboardMetrics metrics) {
        return new DashboardMetricsResponse(
                metrics.aqi().value(),
                metrics.aqi().category().name(),
                metrics.averageCo2(),
                metrics.averagePm2_5(),
                metrics.averageTemperature(),
                metrics.averageHumidity(),
                metrics.co2Trend().deltaPercentage(),
                metrics.pm2_5Trend().deltaPercentage(),
                metrics.temperatureTrend().deltaPercentage(),
                metrics.humidityTrend().deltaPercentage(),
                metrics.calculatedAt()
        );
    }

    public static TrendChartResponse toTrendChartResponse(List<KpiTrendPoint> points) {
        var dataPoints = points.stream()
                .map(p -> new TrendChartResponse.TrendDataPoint(
                        p.timestamp(),
                        p.aqiValue(),
                        p.co2(),
                        p.pm2_5(),
                        p.temperature(),
                        p.humidity()
                ))
                .toList();
        return new TrendChartResponse(dataPoints);
    }
}
