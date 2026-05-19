package com.claircore.evaluation.domain.model.entities;

import com.claircore.evaluation.domain.model.valueobjects.*;
import com.claircore.shared.domain.model.entities.AuditableModel;
import jakarta.persistence.*;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "telemetry_evaluations")
@EntityListeners(AuditingEntityListener.class)
public class TelemetryEvaluation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Embedded
    @AttributeOverride(name = "value", column = @Column(name = "device_id", nullable = false))
    private DeviceId deviceId;

    @Embedded
    @AttributeOverride(name = "value", column = @Column(name = "co2_ppm", nullable = false))
    private Co2Level co2;

    @Embedded
    @AttributeOverride(name = "value", column = @Column(name = "pm25_ug_m3", nullable = false))
    private Pm25Level pm25;

    @Embedded
    @AttributeOverride(name = "value", column = @Column(name = "pm10_ug_m3", nullable = false))
    private Pm10Level pm10;

    @Embedded
    @AttributeOverride(name = "value", column = @Column(name = "temperature_celsius", nullable = false))
    private Temperature temperature;

    @Embedded
    @AttributeOverride(name = "value", column = @Column(name = "humidity_percent", nullable = false))
    private Humidity humidity;

    @Column(name = "air_quality_valid", nullable = false)
    private Boolean airQualityValid;

    @Column(name = "pm_valid", nullable = false)
    private Boolean pmValid;

    @Column(nullable = false)
    private String status;

    @Column(name = "status_code", nullable = false)
    private Integer statusCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "air_quality_status", nullable = false, length = 20)
    private AirQualityStatus airQualityStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "health_state", nullable = false, length = 20)
    private HealthState healthState;

    @ElementCollection
    @CollectionTable(name = "evaluation_threshold_breaches", joinColumns = @JoinColumn(name = "evaluation_id"))
    private List<ThresholdBreach> thresholdBreaches = new ArrayList<>();

    @Column(name = "recorded_at", nullable = false)
    private Instant recordedAt;

    @Embedded
    private EvaluationAudit auditFields = new EvaluationAudit();

    protected TelemetryEvaluation() {}

    public TelemetryEvaluation(
            DeviceId deviceId,
            Co2Level co2,
            Pm25Level pm25,
            Pm10Level pm10,
            Temperature temperature,
            Humidity humidity,
            Boolean airQualityValid,
            Boolean pmValid,
            String status,
            Integer statusCode,
            Instant recordedAt
    ) {
        if (deviceId == null) {
            throw new IllegalArgumentException("Device ID must not be null");
        }
        if (co2 == null) {
            throw new IllegalArgumentException("CO2 level must not be null");
        }
        if (pm25 == null) {
            throw new IllegalArgumentException("PM2.5 level must not be null");
        }
        if (pm10 == null) {
            throw new IllegalArgumentException("PM10 level must not be null");
        }
        if (temperature == null) {
            throw new IllegalArgumentException("Temperature must not be null");
        }
        if (humidity == null) {
            throw new IllegalArgumentException("Humidity must not be null");
        }
        if (airQualityValid == null) {
            throw new IllegalArgumentException("Air quality valid flag must not be null");
        }
        if (pmValid == null) {
            throw new IllegalArgumentException("PM valid flag must not be null");
        }
        if (status == null || status.isBlank()) {
            throw new IllegalArgumentException("Status must not be null or blank");
        }
        if (statusCode == null) {
            throw new IllegalArgumentException("Status code must not be null");
        }
        if (recordedAt == null) {
            throw new IllegalArgumentException("Recorded at must not be null");
        }

        this.deviceId = deviceId;
        this.co2 = co2;
        this.pm25 = pm25;
        this.pm10 = pm10;
        this.temperature = temperature;
        this.humidity = humidity;
        this.airQualityValid = airQualityValid;
        this.pmValid = pmValid;
        this.status = status;
        this.statusCode = statusCode;
        this.recordedAt = recordedAt;

        this.thresholdBreaches = computeThresholdBreaches();
        this.airQualityStatus = computeAirQualityStatus();
        this.healthState = computeHealthState();
    }

    private List<ThresholdBreach> computeThresholdBreaches() {
        List<ThresholdBreach> breaches = new ArrayList<>();

        double co2Value = this.co2.value();
        if (co2Value > 2000) {
            breaches.add(new ThresholdBreach("co2", co2Value, 2000.0));
        } else if (co2Value > 1000) {
            breaches.add(new ThresholdBreach("co2", co2Value, 1000.0));
        }

        double pm25Value = this.pm25.value();
        if (pm25Value > 75) {
            breaches.add(new ThresholdBreach("pm25", pm25Value, 75.0));
        } else if (pm25Value > 35) {
            breaches.add(new ThresholdBreach("pm25", pm25Value, 35.0));
        }

        double pm10Value = this.pm10.value();
        if (pm10Value > 250) {
            breaches.add(new ThresholdBreach("pm10", pm10Value, 250.0));
        } else if (pm10Value > 150) {
            breaches.add(new ThresholdBreach("pm10", pm10Value, 150.0));
        }

        return breaches;
    }

    private AirQualityStatus computeAirQualityStatus() {
        double co2Value = this.co2.value();
        double pm25Value = this.pm25.value();
        double pm10Value = this.pm10.value();

        boolean hasHazardous = co2Value > 2000 || pm25Value > 75 || pm10Value > 250;
        boolean hasUnhealthy = co2Value > 1000 || pm25Value > 35 || pm10Value > 150;

        if (hasHazardous) {
            return AirQualityStatus.HAZARDOUS;
        }
        if (hasUnhealthy) {
            return AirQualityStatus.UNHEALTHY;
        }
        return AirQualityStatus.GOOD;
    }

    private HealthState computeHealthState() {
        boolean hasCritical = thresholdBreaches.stream()
                .anyMatch(b -> {
                    return switch (b.metric()) {
                        case "co2" -> b.value() > 2000;
                        case "pm25" -> b.value() > 75;
                        case "pm10" -> b.value() > 250;
                        default -> false;
                    };
                });

        if (hasCritical) {
            return HealthState.CRITICAL;
        }

        boolean hasWarning = !thresholdBreaches.isEmpty();
        boolean sensorsInvalid = !this.airQualityValid || !this.pmValid;
        boolean deviceError = this.statusCode != 0;

        if (hasWarning || sensorsInvalid || deviceError) {
            return HealthState.DEGRADED;
        }

        return HealthState.OPTIMAL;
    }

    public UUID getId() { return id; }
    public DeviceId getDeviceId() { return deviceId; }
    public Co2Level getCo2() { return co2; }
    public Pm25Level getPm25() { return pm25; }
    public Pm10Level getPm10() { return pm10; }
    public Temperature getTemperature() { return temperature; }
    public Humidity getHumidity() { return humidity; }
    public Boolean getAirQualityValid() { return airQualityValid; }
    public Boolean getPmValid() { return pmValid; }
    public String getStatus() { return status; }
    public Integer getStatusCode() { return statusCode; }
    public AirQualityStatus getAirQualityStatus() { return airQualityStatus; }
    public HealthState getHealthState() { return healthState; }
    public List<ThresholdBreach> getThresholdBreaches() { return thresholdBreaches; }
    public Instant getRecordedAt() { return recordedAt; }
    public EvaluationAudit getAuditFields() { return auditFields; }

    @Embeddable
    public static class EvaluationAudit extends AuditableModel {}
}
