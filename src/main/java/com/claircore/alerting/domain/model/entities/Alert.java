package com.claircore.alerting.domain.model.entities;

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

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    @Embedded
    private AlertAudit auditFields = new AlertAudit();

    protected Alert() {}

    public Alert(UUID deviceId, UUID spaceId, MetricType metric, BigDecimal thresholdValue, BigDecimal actualValue, String message, Instant occurredAt) {
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
        if (occurredAt == null) {
            throw new IllegalArgumentException("Occurred at must not be null");
        }

        this.deviceId = deviceId;
        this.spaceId = spaceId;
        this.metric = metric;
        this.thresholdValue = thresholdValue;
        this.actualValue = actualValue;
        this.message = message;
        this.status = AlertStatus.ACTIVE;
        this.occurredAt = occurredAt;
    }

    public void acknowledge() {
        this.status = AlertStatus.ACKNOWLEDGED;
    }

    public void resolve() {
        this.status = AlertStatus.RESOLVED;
    }

    public UUID getId() { return id; }
    public UUID getDeviceId() { return deviceId; }
    public UUID getSpaceId() { return spaceId; }
    public MetricType getMetric() { return metric; }
    public BigDecimal getThresholdValue() { return thresholdValue; }
    public BigDecimal getActualValue() { return actualValue; }
    public String getMessage() { return message; }
    public AlertStatus getStatus() { return status; }
    public Instant getOccurredAt() { return occurredAt; }
    public AlertAudit getAuditFields() { return auditFields; }

    @Embeddable
    public static class AlertAudit extends AuditableModel {}
}
