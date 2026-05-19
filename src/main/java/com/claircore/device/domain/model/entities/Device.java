package com.claircore.device.domain.model.entities;

import com.claircore.device.domain.model.valueobjects.*;
import com.claircore.shared.domain.model.entities.AuditableModel;
import jakarta.persistence.*;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.util.UUID;

@Entity
@Table(name = "devices")
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

    @Embedded
    @AttributeOverride(name = "value", column = @Column(name = "hardware_id", nullable = false, unique = true))
    private HardwareId hardwareId;

    @Embedded
    @AttributeOverride(name = "value", column = @Column(name = "api_key", nullable = false, unique = true, length = 255))
    private ApiKey apiKey;

    @Embedded
    @AttributeOverride(name = "value", column = @Column(name = "device_secret", nullable = false, unique = true, length = 255))
    private DeviceSecret deviceSecret;

    @Embedded
    @AttributeOverride(name = "value", column = @Column(name = "device_type", nullable = false))
    private DeviceType deviceType;

    @Embedded
    private DeviceAudit auditFields = new DeviceAudit();

    protected Device() {}

    public Device(String serialNumber, String name, HardwareId hardwareId, ApiKey apiKey, DeviceSecret deviceSecret, DeviceType deviceType) {
        this.serialNumber = serialNumber;
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Device name must not be null or blank");
        }
        this.name = name;
        this.factoryName = name;
        this.hardwareId = hardwareId;
        this.apiKey = apiKey;
        this.deviceSecret = deviceSecret;
        this.deviceType = deviceType;
    }

    public void rotateApiKey(ApiKey apiKey) {
        this.apiKey = apiKey;
    }

    public void updateName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Device name must not be null or blank");
        }
        this.name = name;
    }

    public void resetNameToFactoryDefault() {
        this.name = this.factoryName;
    }

    public UUID getId() { return id; }
    public String getSerialNumber() { return serialNumber; }
    public String getName() { return name; }
    public String getFactoryName() { return factoryName; }
    public HardwareId getHardwareId() { return hardwareId; }
    public ApiKey getApiKey() { return apiKey; }
    public DeviceSecret getDeviceSecret() { return deviceSecret; }
    public DeviceType getDeviceType() { return deviceType; }
    public DeviceAudit getAuditFields() { return auditFields; }

    @Embeddable
    public static class DeviceAudit extends AuditableModel {}
}
