package com.claircore.analytics.domain.services;

import com.claircore.analytics.domain.model.valueobjects.AggregatedMetrics;
import com.claircore.analytics.domain.model.valueobjects.AirQualityIndex;
import com.claircore.analytics.domain.model.valueobjects.DeviceMetricsSnapshot;
import com.claircore.analytics.domain.model.valueobjects.Freshness;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
public class MetricsAggregationDomainServiceImpl implements MetricsAggregationDomainService {

    private final AqiCalculationDomainService aqiCalculationDomainService;

    public MetricsAggregationDomainServiceImpl(AqiCalculationDomainService aqiCalculationDomainService) {
        this.aqiCalculationDomainService = aqiCalculationDomainService;
    }

    @Override
    public AggregatedMetrics aggregate(List<DeviceMetricsSnapshot> snapshots) {
        if (snapshots == null || snapshots.isEmpty()) {
            return emptyMetrics();
        }

        List<Double> co2 = new ArrayList<>();
        List<Double> pm25 = new ArrayList<>();
        List<Double> temp = new ArrayList<>();
        List<Double> hum = new ArrayList<>();
        List<Integer> aqi = new ArrayList<>();
        List<Double> co2Delta = new ArrayList<>();
        List<Double> pm25Delta = new ArrayList<>();
        List<Double> tempDelta = new ArrayList<>();
        List<Double> humDelta = new ArrayList<>();
        Instant latest = null;
        boolean hasLive = false;
        boolean hasSnapshot = false;

        for (DeviceMetricsSnapshot snapshot : snapshots) {
            if (snapshot.source() == DeviceMetricsSnapshot.Source.LIVE) hasLive = true;
            if (snapshot.source() == DeviceMetricsSnapshot.Source.SNAPSHOT) hasSnapshot = true;

            if (isValid(snapshot.averageCo2())) co2.add(snapshot.averageCo2());
            if (isValid(snapshot.averagePm2_5())) pm25.add(snapshot.averagePm2_5());
            if (isValid(snapshot.averageTemperature())) temp.add(snapshot.averageTemperature());
            if (isValid(snapshot.averageHumidity())) hum.add(snapshot.averageHumidity());
            if (snapshot.aqiValue() != null) aqi.add(snapshot.aqiValue());
            if (isValid(snapshot.co2DeltaPercentage())) co2Delta.add(snapshot.co2DeltaPercentage());
            if (isValid(snapshot.pm2_5DeltaPercentage())) pm25Delta.add(snapshot.pm2_5DeltaPercentage());
            if (isValid(snapshot.temperatureDeltaPercentage())) tempDelta.add(snapshot.temperatureDeltaPercentage());
            if (isValid(snapshot.humidityDeltaPercentage())) humDelta.add(snapshot.humidityDeltaPercentage());

            if (snapshot.recordedAt() != null) {
                latest = (latest == null || snapshot.recordedAt().isAfter(latest)) ? snapshot.recordedAt() : latest;
            }
        }

        Double avgCo2 = average(co2);
        Double avgPm25 = average(pm25);
        Double avgTemp = average(temp);
        Double avgHum = average(hum);
        Double avgCo2Delta = average(co2Delta);
        Double avgPm25Delta = average(pm25Delta);
        Double avgTempDelta = average(tempDelta);
        Double avgHumDelta = average(humDelta);

        Integer avgAqi = averageInt(aqi);
        String aqiCategory = null;
        if (avgAqi != null && avgPm25 != null && avgCo2 != null) {
            AirQualityIndex derived = aqiCalculationDomainService.calculateAqi(avgPm25, avgCo2);
            avgAqi = derived.value();
            aqiCategory = derived.category().name();
        }

        Freshness freshness = Freshness.NO_DATA;
        if (hasLive) freshness = Freshness.LIVE;
        else if (hasSnapshot) freshness = Freshness.STALE;

        if (avgAqi == null && avgCo2 == null && avgPm25 == null && avgTemp == null && avgHum == null) {
            latest = null;
        }

        return new AggregatedMetrics(
                avgAqi, aqiCategory,
                round1(avgCo2), round1(avgPm25), round1(avgTemp), round1(avgHum),
                round1(avgCo2Delta), round1(avgPm25Delta), round1(avgTempDelta), round1(avgHumDelta),
                latest, freshness
        );
    }

    private AggregatedMetrics emptyMetrics() {
        return new AggregatedMetrics(null, null, null, null, null, null, null, null, null, null, null, Freshness.NO_DATA);
    }

    private boolean isValid(Double value) {
        return value != null && Double.isFinite(value);
    }

    private Double average(List<Double> values) {
        if (values.isEmpty()) return null;
        return values.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
    }

    private Integer averageInt(List<Integer> values) {
        if (values.isEmpty()) return null;
        double avg = values.stream().mapToInt(Integer::intValue).average().orElse(0.0);
        return (int) Math.round(avg);
    }

    private Double round1(Double value) {
        if (value == null) return null;
        return Math.round(value * 10.0) / 10.0;
    }
}
