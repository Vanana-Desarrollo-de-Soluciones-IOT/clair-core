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
    @AttributeOverride(name = "valid", column = @Column(name = "aq_valid", nullable = false))
    private AirQuality airQuality;

    @Embedded
    @AttributeOverride(name = "pm1_0", column = @Column(name = "pm_pm1_0", nullable = false))
    @AttributeOverride(name = "pm2_5", column = @Column(name = "pm_pm2_5", nullable = false))
    @AttributeOverride(name = "pm10", column = @Column(name = "pm_pm10", nullable = false))
    @AttributeOverride(name = "valid", column = @Column(name = "pm_valid", nullable = false))
    private ParticulateMatter particulateMatter;

    @Embedded
    @AttributeOverride(name = "status", column = @Column(name = "conn_status", nullable = false))
    @AttributeOverride(name = "ssid", column = @Column(name = "conn_ssid", nullable = false))
    @AttributeOverride(name = "ip", column = @Column(name = "conn_ip", nullable = false))
    @AttributeOverride(name = "rssi", column = @Column(name = "conn_rssi", nullable = false))
    @AttributeOverride(name = "mac", column = @Column(name = "conn_mac", nullable = false))
    @AttributeOverride(name = "channel", column = @Column(name = "conn_channel", nullable = false))
    private Connectivity connectivity;

    @Embedded
    @AttributeOverride(name = "freeHeap", column = @Column(name = "dh_free_heap", nullable = false))
    @AttributeOverride(name = "minFreeHeap", column = @Column(name = "dh_min_free_heap", nullable = false))
    @AttributeOverride(name = "heapSize", column = @Column(name = "dh_heap_size", nullable = false))
    @AttributeOverride(name = "maxAllocHeap", column = @Column(name = "dh_max_alloc_heap", nullable = false))
    @AttributeOverride(name = "scd41Status", column = @Column(name = "dh_scd41_status", nullable = false))
    @AttributeOverride(name = "pms5003Status", column = @Column(name = "dh_pms5003_status", nullable = false))
    @AttributeOverride(name = "lastValidAirQualitySec", column = @Column(name = "dh_last_valid_aq_sec", nullable = false))
    @AttributeOverride(name = "lastValidPMSec", column = @Column(name = "dh_last_valid_pm_sec", nullable = false))
    private DeviceHealth deviceHealth;

    @Embedded
    @AttributeOverride(name = "chipModel", column = @Column(name = "di_chip_model", nullable = false))
    @AttributeOverride(name = "chipRevision", column = @Column(name = "di_chip_revision", nullable = false))
    @AttributeOverride(name = "cpuFreqMHz", column = @Column(name = "di_cpu_freq_mhz", nullable = false))
    @AttributeOverride(name = "flashSize", column = @Column(name = "di_flash_size", nullable = false))
    @AttributeOverride(name = "sketchSize", column = @Column(name = "di_sketch_size", nullable = false))
    @AttributeOverride(name = "freeSketchSpace", column = @Column(name = "di_free_sketch_space", nullable = false))
    private DeviceInfo deviceInfo;

    @Column(name = "device_timestamp", nullable = false)
    private Long deviceTimestamp;

    @Column(name = "uptime_seconds", nullable = false)
    private Integer uptimeSeconds;

    @Column(nullable = false)
    private String status;

    @Column(name = "status_code", nullable = false)
    private Integer statusCode;

    @Column(name = "recorded_at", nullable = false)
    private Instant recordedAt;

    @Embedded
    private EvaluationAudit auditFields = new EvaluationAudit();

    protected TelemetryEvaluation() {}

    public TelemetryEvaluation(
            DeviceId deviceId,
            Long deviceTimestamp,
            Integer uptimeSeconds,
            AirQuality airQuality,
            ParticulateMatter particulateMatter,
            Connectivity connectivity,
            DeviceHealth deviceHealth,
            DeviceInfo deviceInfo,
            String status,
            Integer statusCode,
            Instant recordedAt
    ) {
        if (deviceId == null) {
            throw new IllegalArgumentException("Device ID must not be null");
        }
        if (deviceTimestamp == null) {
            throw new IllegalArgumentException("deviceTimestamp must not be null");
        }
        if (uptimeSeconds == null) {
            throw new IllegalArgumentException("uptimeSeconds must not be null");
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
        if (deviceHealth == null) {
            throw new IllegalArgumentException("deviceHealth must not be null");
        }
        if (deviceInfo == null) {
            throw new IllegalArgumentException("deviceInfo must not be null");
        }
        if (status == null || status.isBlank()) {
            throw new IllegalArgumentException("status must not be null or blank");
        }
        if (statusCode == null) {
            throw new IllegalArgumentException("statusCode must not be null");
        }
        if (recordedAt == null) {
            throw new IllegalArgumentException("recordedAt must not be null");
        }

        this.deviceId = deviceId;
        this.deviceTimestamp = deviceTimestamp;
        this.uptimeSeconds = uptimeSeconds;
        this.airQuality = airQuality;
        this.particulateMatter = particulateMatter;
        this.connectivity = connectivity;
        this.deviceHealth = deviceHealth;
        this.deviceInfo = deviceInfo;
        this.status = status;
        this.statusCode = statusCode;
        this.recordedAt = recordedAt;
    }

    public UUID getId() { return id; }
    public DeviceId getDeviceId() { return deviceId; }
    public AirQuality getAirQuality() { return airQuality; }
    public ParticulateMatter getParticulateMatter() { return particulateMatter; }
    public Connectivity getConnectivity() { return connectivity; }
    public DeviceHealth getDeviceHealth() { return deviceHealth; }
    public DeviceInfo getDeviceInfo() { return deviceInfo; }
    public Long getDeviceTimestamp() { return deviceTimestamp; }
    public Integer getUptimeSeconds() { return uptimeSeconds; }
    public String getStatus() { return status; }
    public Integer getStatusCode() { return statusCode; }
    public Instant getRecordedAt() { return recordedAt; }
    public EvaluationAudit getAuditFields() { return auditFields; }

    @Embeddable
    public static class EvaluationAudit extends AuditableModel {}
}
