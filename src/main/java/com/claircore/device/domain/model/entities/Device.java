package com.claircore.device.domain.model.entities;

import com.claircore.device.domain.model.valueobjects.*;
import com.claircore.shared.domain.model.entities.AuditableModel;
import jakarta.persistence.*;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.util.UUID;

@Entity
@Table(name = "devices", indexes = @Index(name = "idx_devices_updated_at", columnList = "updated_at"))
@EntityListeners(AuditingEntityListener.class)
public class Device {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true)
    private String serialNumber;

    @Column(nullable = false)
    private String name;

    @Column(name = "factory_name", nullable = false)
    private String factoryName;

    // Nullable keeps ddl-auto update compatible with existing PostgreSQL rows; new entities default false.
    @Column(name = "deleted")
    private boolean deleted = false;

    @Embedded
    @AttributeOverride(name = "value", column = @Column(name = "hardware_id", nullable = false, unique = true))
    private HardwareId hardwareId;

    @Embedded
    @AttributeOverride(name = "value", column = @Column(name = "api_key", nullable = false, unique = true, length = 255))
    private ApiKey apiKey;

    @Embedded
    @AttributeOverride(name = "value", column = @Column(name = "device_type", nullable = false))
    private DeviceType deviceType;

    @Embedded
    private DeviceAudit auditFields = new DeviceAudit();

    protected Device() {}

    public Device(String serialNumber, String name, HardwareId hardwareId, ApiKey apiKey, DeviceType deviceType) {
        this.serialNumber = serialNumber;
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Device name must not be null or blank");
        }
        this.name = name;
        this.factoryName = name;
        this.hardwareId = hardwareId;
        this.apiKey = apiKey;
        this.deviceType = deviceType;
    }

    public void rotateApiKey(ApiKey apiKey) {
        this.apiKey = apiKey;
        auditFields.touchUpdatedAt();
    }

    public void updateName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Device name must not be null or blank");
        }
        this.name = name;
        auditFields.touchUpdatedAt();
    }

    public void resetNameToFactoryDefault() {
        this.name = this.factoryName;
        auditFields.touchUpdatedAt();
    }

    /** Marks the device as decommissioned while retaining its row as a roster tombstone. */
    public void markDeleted() {
        if (!this.deleted) {
            this.deleted = true;
            auditFields.touchUpdatedAt();
        }
    }

    public boolean isDeleted() { return deleted; }

    public UUID getId() { return id; }
    public String getSerialNumber() { return serialNumber; }
    public String getName() { return name; }
    public String getFactoryName() { return factoryName; }
    public HardwareId getHardwareId() { return hardwareId; }
    public ApiKey getApiKey() { return apiKey; }
    public DeviceType getDeviceType() { return deviceType; }
    public DeviceAudit getAuditFields() { return auditFields; }

    @Embeddable
    public static class DeviceAudit extends AuditableModel {}
}
