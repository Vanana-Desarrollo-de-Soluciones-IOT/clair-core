package com.claircore.analytics.domain.model.entities;

import com.claircore.analytics.domain.model.valueobjects.AirQualityIndex;
import com.claircore.analytics.domain.model.valueobjects.DeviceId;
import com.claircore.shared.domain.model.entities.AuditableModel;
import jakarta.persistence.*;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "device_analytics_snapshots")
@EntityListeners(AuditingEntityListener.class)
public class DeviceAnalyticsSnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Embedded
    @AttributeOverride(name = "value", column = @Column(name = "device_id", nullable = false))
    private DeviceId deviceId;

    @Column(name = "time_window_start", nullable = false)
    private Instant timeWindowStart;

    @Column(name = "time_window_end", nullable = false)
    private Instant timeWindowEnd;

    @Column(name = "average_co2", nullable = false)
    private Double averageCo2;

    @Column(name = "average_pm2_5", nullable = false)
    private Double averagePm2_5;

    @Column(name = "average_temperature", nullable = false)
    private Double averageTemperature;

    @Column(name = "average_humidity", nullable = false)
    private Double averageHumidity;

    @Embedded
    @AttributeOverride(name = "value", column = @Column(name = "aqi_value", nullable = false))
    @AttributeOverride(name = "category", column = @Column(name = "aqi_category", nullable = false))
    private AirQualityIndex calculatedAqi;

    @Embedded
    private SnapshotAudit auditFields = new SnapshotAudit();

    protected DeviceAnalyticsSnapshot() {}

    public DeviceAnalyticsSnapshot(
            DeviceId deviceId,
            Instant timeWindowStart,
            Instant timeWindowEnd,
            Double averageCo2,
            Double averagePm2_5,
            Double averageTemperature,
            Double averageHumidity,
            AirQualityIndex calculatedAqi
    ) {
        if (deviceId == null) {
            throw new IllegalArgumentException("Device ID must not be null");
        }
        if (timeWindowStart == null) {
            throw new IllegalArgumentException("timeWindowStart must not be null");
        }
        if (timeWindowEnd == null) {
            throw new IllegalArgumentException("timeWindowEnd must not be null");
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
        if (calculatedAqi == null) {
            throw new IllegalArgumentException("calculatedAqi must not be null");
        }

        this.deviceId = deviceId;
        this.timeWindowStart = timeWindowStart;
        this.timeWindowEnd = timeWindowEnd;
        this.averageCo2 = averageCo2;
        this.averagePm2_5 = averagePm2_5;
        this.averageTemperature = averageTemperature;
        this.averageHumidity = averageHumidity;
        this.calculatedAqi = calculatedAqi;
    }

    public UUID getId() { return id; }
    public DeviceId getDeviceId() { return deviceId; }
    public Instant getTimeWindowStart() { return timeWindowStart; }
    public Instant getTimeWindowEnd() { return timeWindowEnd; }
    public Double getAverageCo2() { return averageCo2; }
    public Double getAveragePm2_5() { return averagePm2_5; }
    public Double getAverageTemperature() { return averageTemperature; }
    public Double getAverageHumidity() { return averageHumidity; }
    public AirQualityIndex getCalculatedAqi() { return calculatedAqi; }
    public SnapshotAudit getAuditFields() { return auditFields; }

    @Embeddable
    public static class SnapshotAudit extends AuditableModel {}
}
