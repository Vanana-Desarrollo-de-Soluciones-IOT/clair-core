package com.claircore.alerting.domain.model.entities;

import com.claircore.alerting.domain.model.valueobjects.AlertSeverity;
import com.claircore.alerting.domain.model.valueobjects.AlertStatus;
import com.claircore.alerting.domain.model.valueobjects.MetricType;
import com.claircore.shared.domain.model.entities.AuditableModel;
import jakarta.persistence.*;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "alerts")
@EntityListeners(AuditingEntityListener.class)
public class Alert {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID deviceId;

    @Column(name = "space_id")
    private UUID spaceId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MetricType metric;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal thresholdValue;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal actualValue;

    @Column(nullable = false, length = 500)
    private String message;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AlertStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AlertSeverity severity;

    @Column(name = "space_name")
    private String spaceName;

    @Column(name = "device_name")
    private String deviceName;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    @Column(name = "resolved_at")
    private Instant resolvedAt;

    @Embedded
    private AlertAudit auditFields = new AlertAudit();

    protected Alert() {}

    public Alert(UUID deviceId, UUID spaceId, String spaceName, String deviceName,
                 MetricType metric, BigDecimal thresholdValue, BigDecimal actualValue,
                 String message, AlertSeverity severity, Instant occurredAt) {
        if (deviceId == null) {
            throw new IllegalArgumentException("Device ID must not be null");
        }
        if (metric == null) {
            throw new IllegalArgumentException("Metric must not be null");
        }
        if (thresholdValue == null || actualValue == null) {
            throw new IllegalArgumentException("Threshold and actual values must not be null");
        }
        if (message == null || message.isBlank()) {
            throw new IllegalArgumentException("Message must not be null or blank");
        }
        if (severity == null) {
            throw new IllegalArgumentException("Severity must not be null");
        }
        if (occurredAt == null) {
            throw new IllegalArgumentException("Occurred at must not be null");
        }

        this.deviceId = deviceId;
        this.spaceId = spaceId;
        this.spaceName = spaceName;
        this.deviceName = deviceName;
        this.metric = metric;
        this.thresholdValue = thresholdValue;
        this.actualValue = actualValue;
        this.message = message;
        this.status = AlertStatus.ACTIVE;
        this.severity = severity;
        this.occurredAt = occurredAt;
    }

    public void acknowledge() {
        this.status = AlertStatus.ACKNOWLEDGED;
    }

    public void resolve(Instant resolvedAt) {
        if (resolvedAt == null) {
            throw new IllegalArgumentException("Resolved at must not be null");
        }
        this.status = AlertStatus.RESOLVED;
        this.resolvedAt = resolvedAt;
    }

    public UUID getId() { return id; }
    public UUID getDeviceId() { return deviceId; }
    public UUID getSpaceId() { return spaceId; }
    public MetricType getMetric() { return metric; }
    public BigDecimal getThresholdValue() { return thresholdValue; }
    public BigDecimal getActualValue() { return actualValue; }
    public String getMessage() { return message; }
    public AlertStatus getStatus() { return status; }
    public AlertSeverity getSeverity() { return severity; }
    public String getSpaceName() { return spaceName; }
    public String getDeviceName() { return deviceName; }
    public Instant getOccurredAt() { return occurredAt; }
    public Instant getResolvedAt() { return resolvedAt; }
    public AlertAudit getAuditFields() { return auditFields; }

    @Embeddable
    public static class AlertAudit extends AuditableModel {}
}
