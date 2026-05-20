package com.claircore.device.domain.model.entities;

import com.claircore.device.domain.model.valueobjects.DeviceCommandStatus;
import com.claircore.device.domain.model.valueobjects.DeviceCommandType;
import com.claircore.shared.domain.model.entities.AuditableModel;
import jakarta.persistence.*;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "device_commands")
@EntityListeners(AuditingEntityListener.class)
public class DeviceCommand {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "device_id", nullable = false)
    private Device device;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DeviceCommandType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DeviceCommandStatus status;

    @Column(columnDefinition = "text")
    private String payload;

    @Column(name = "sent_at")
    private Instant sentAt;

    @Column(name = "executed_at")
    private Instant executedAt;

    @Column(name = "failure_reason", columnDefinition = "text")
    private String failureReason;

    @Embedded
    private DeviceCommandAudit auditFields = new DeviceCommandAudit();

    protected DeviceCommand() {}

    public DeviceCommand(Device device, DeviceCommandType type, String payload) {
        if (device == null) {
            throw new IllegalArgumentException("Device must not be null");
        }
        if (type == null) {
            throw new IllegalArgumentException("Device command type must not be null");
        }
        this.device = device;
        this.type = type;
        this.payload = payload;
        this.status = DeviceCommandStatus.PENDING;
    }

    public void markSent() {
        if (this.status == DeviceCommandStatus.PENDING) {
            this.status = DeviceCommandStatus.SENT;
            this.sentAt = Instant.now();
        }
    }

    public void markExecuted() {
        this.status = DeviceCommandStatus.EXECUTED;
        this.executedAt = Instant.now();
        this.failureReason = null;
    }

    public void markFailed(String failureReason) {
        this.status = DeviceCommandStatus.FAILED;
        this.executedAt = Instant.now();
        this.failureReason = failureReason;
    }

    public UUID getId() { return id; }
    public Device getDevice() { return device; }
    public DeviceCommandType getType() { return type; }
    public DeviceCommandStatus getStatus() { return status; }
    public String getPayload() { return payload; }
    public Instant getSentAt() { return sentAt; }
    public Instant getExecutedAt() { return executedAt; }
    public String getFailureReason() { return failureReason; }
    public DeviceCommandAudit getAuditFields() { return auditFields; }

    @Embeddable
    public static class DeviceCommandAudit extends AuditableModel {}
}
