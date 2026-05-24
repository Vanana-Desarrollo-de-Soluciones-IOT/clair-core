package com.claircore.device.domain.model.entities;

import com.claircore.device.domain.model.valueobjects.MetricThreshold;
import com.claircore.device.domain.model.valueobjects.ThresholdOperator;
import com.claircore.shared.domain.model.entities.AuditableModel;
import jakarta.persistence.*;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "device_thresholds",
        uniqueConstraints = @UniqueConstraint(columnNames = {"assignment_id", "metric"}))
@EntityListeners(AuditingEntityListener.class)
public class DeviceThreshold {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "assignment_id", nullable = false)
    private UUID assignmentId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MetricThreshold metric;

    @Enumerated(EnumType.STRING)
    @Column(name = "operator", nullable = false, length = 20)
    private ThresholdOperator operator;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal value;

    @Column(nullable = false)
    private boolean enabled = true;

    @Embedded
    private DeviceThresholdAudit auditFields = new DeviceThresholdAudit();

    protected DeviceThreshold() {}

    public DeviceThreshold(UUID assignmentId, MetricThreshold metric, ThresholdOperator operator, BigDecimal value) {
        if (assignmentId == null) {
            throw new IllegalArgumentException("Assignment ID must not be null");
        }
        if (metric == null) {
            throw new IllegalArgumentException("Metric must not be null");
        }
        if (operator == null) {
            throw new IllegalArgumentException("Operator must not be null");
        }
        if (value == null) {
            throw new IllegalArgumentException("Value must not be null");
        }

        this.assignmentId = assignmentId;
        this.metric = metric;
        this.operator = operator;
        this.value = value;
        this.enabled = true;
    }

    public void update(ThresholdOperator operator, BigDecimal value) {
        if (operator == null || value == null) {
            throw new IllegalArgumentException("Operator and value must not be null");
        }
        this.operator = operator;
        this.value = value;
    }

    public void toggle(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean evaluate(BigDecimal telemetryValue) {
        if (!enabled || telemetryValue == null) {
            return false;
        }
        return switch (operator) {
            case GREATER_THAN -> telemetryValue.compareTo(value) > 0;
            case LESS_THAN -> telemetryValue.compareTo(value) < 0;
            case EQUALS -> telemetryValue.compareTo(value) == 0;
            case GREATER_THAN_OR_EQUALS -> telemetryValue.compareTo(value) >= 0;
            case LESS_THAN_OR_EQUALS -> telemetryValue.compareTo(value) <= 0;
        };
    }

    public UUID getId() { return id; }
    public UUID getAssignmentId() { return assignmentId; }
    public MetricThreshold getMetric() { return metric; }
    public ThresholdOperator getOperator() { return operator; }
    public BigDecimal getValue() { return value; }
    public boolean isEnabled() { return enabled; }
    public DeviceThresholdAudit getAuditFields() { return auditFields; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DeviceThreshold that = (DeviceThreshold) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Embeddable
    public static class DeviceThresholdAudit extends AuditableModel {}
}