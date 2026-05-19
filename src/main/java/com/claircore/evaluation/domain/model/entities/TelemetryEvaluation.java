package com.claircore.evaluation.domain.model.entities;

import com.claircore.evaluation.domain.model.valueobjects.*;
import com.claircore.shared.domain.model.entities.AuditableModel;
import jakarta.persistence.*;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
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
    @AttributeOverride(name = "co2", column = @Column(name = "aq_co2", nullable = false))
    @AttributeOverride(name = "temperature", column = @Column(name = "aq_temperature", nullable = false))
    @AttributeOverride(name = "humidity", column = @Column(name = "aq_humidity", nullable = false))
    private AirQuality airQuality;

    @Embedded
    @AttributeOverride(name = "pm1_0", column = @Column(name = "pm_pm1_0", nullable = false))
    @AttributeOverride(name = "pm2_5", column = @Column(name = "pm_pm2_5", nullable = false))
    @AttributeOverride(name = "pm10", column = @Column(name = "pm_pm10", nullable = false))
    private ParticulateMatter particulateMatter;

    @Embedded
    @AttributeOverride(name = "status", column = @Column(name = "conn_status", nullable = false))
    @AttributeOverride(name = "network", column = @Column(name = "conn_network"))
    @AttributeOverride(name = "signalStrength", column = @Column(name = "conn_signal_strength"))
    private Connectivity connectivity;

    @Embedded
    @AttributeOverride(name = "country", column = @Column(name = "location_country"))
    private Location location;

    @Column(name = "device_time", nullable = false)
    private String deviceTime;

    @Column(name = "uptime", nullable = false)
    private String uptime;

    @Column(nullable = false)
    private String status;

    @Column(name = "health_status", nullable = false)
    private Integer healthStatus;

    @Column(name = "recorded_at", nullable = false)
    private Instant recordedAt;

    @Embedded
    private EvaluationAudit auditFields = new EvaluationAudit();

    protected TelemetryEvaluation() {}

    public TelemetryEvaluation(
            DeviceId deviceId,
            String deviceTime,
            String uptime,
            AirQuality airQuality,
            ParticulateMatter particulateMatter,
            Connectivity connectivity,
            Location location,
            Integer healthStatus,
            String status,
            Instant recordedAt
    ) {
        if (deviceId == null) {
            throw new IllegalArgumentException("Device ID must not be null");
        }
        if (deviceTime == null || deviceTime.isBlank()) {
            throw new IllegalArgumentException("deviceTime must not be null or blank");
        }
        if (uptime == null || uptime.isBlank()) {
            throw new IllegalArgumentException("uptime must not be null or blank");
        }
        if (airQuality == null) {
            throw new IllegalArgumentException("airQuality must not be null");
        }
        if (particulateMatter == null) {
            throw new IllegalArgumentException("particulateMatter must not be null");
        }
        if (connectivity == null) {
            throw new IllegalArgumentException("connectivity must not be null");
        }
        if (location == null) {
            throw new IllegalArgumentException("location must not be null");
        }
        if (healthStatus == null || healthStatus < 0 || healthStatus > 100) {
            throw new IllegalArgumentException("healthStatus must be between 0 and 100");
        }
        if (status == null || status.isBlank()) {
            throw new IllegalArgumentException("status must not be null or blank");
        }
        if (recordedAt == null) {
            throw new IllegalArgumentException("recordedAt must not be null");
        }

        this.deviceId = deviceId;
        this.deviceTime = deviceTime;
        this.uptime = uptime;
        this.airQuality = airQuality;
        this.particulateMatter = particulateMatter;
        this.connectivity = connectivity;
        this.location = location;
        this.healthStatus = healthStatus;
        this.status = status;
        this.recordedAt = recordedAt;
    }

    public UUID getId() { return id; }
    public DeviceId getDeviceId() { return deviceId; }
    public AirQuality getAirQuality() { return airQuality; }
    public ParticulateMatter getParticulateMatter() { return particulateMatter; }
    public Connectivity getConnectivity() { return connectivity; }
    public Location getLocation() { return location; }
    public String getDeviceTime() { return deviceTime; }
    public String getUptime() { return uptime; }
    public Integer getHealthStatus() { return healthStatus; }
    public String getStatus() { return status; }
    public Instant getRecordedAt() { return recordedAt; }
    public EvaluationAudit getAuditFields() { return auditFields; }

    @Embeddable
    public static class EvaluationAudit extends AuditableModel {}
}
