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

    @Embedded
    @AttributeOverride(name = "value", column = @Column(name = "hardware_id", nullable = false, unique = true))
    private HardwareId hardwareId;

    @Embedded
    @AttributeOverride(name = "value", column = @Column(name = "api_key", nullable = false, unique = true))
    private ApiKey apiKey;

    @Embedded
    @AttributeOverride(name = "value", column = @Column(name = "device_type", nullable = false))
    private DeviceType deviceType;

    @Embedded
    private DeviceAudit auditFields = new DeviceAudit();

    protected Device() {}

    public Device(String serialNumber, String name, HardwareId hardwareId, ApiKey apiKey, DeviceType deviceType) {
        this.serialNumber = serialNumber;
        this.name = name;
        this.hardwareId = hardwareId;
        this.apiKey = apiKey;
        this.deviceType = deviceType;
    }

    public UUID getId() { return id; }
    public String getSerialNumber() { return serialNumber; }
    public String getName() { return name; }
    public HardwareId getHardwareId() { return hardwareId; }
    public ApiKey getApiKey() { return apiKey; }
    public DeviceType getDeviceType() { return deviceType; }
    public DeviceAudit getAuditFields() { return auditFields; }

    @Embeddable
    public static class DeviceAudit extends AuditableModel {}
}
