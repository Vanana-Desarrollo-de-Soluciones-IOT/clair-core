package com.claircore.device.domain.model.entities;

import com.claircore.device.domain.model.valueobjects.DeviceStatus;
import com.claircore.shared.domain.model.entities.AuditableModel;
import jakarta.persistence.*;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.util.HashMap;
import java.util.Map;
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

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DeviceStatus status;

    @Column(name = "space_id", nullable = false)
    private UUID spaceId;

    @ElementCollection
    @CollectionTable(name = "device_configuration", joinColumns = @JoinColumn(name = "device_id"))
    @MapKeyColumn(name = "config_key")
    @Column(name = "config_value")
    private Map<String, String> configuration = new HashMap<>();

    @Embedded
    private DeviceAudit auditFields = new DeviceAudit();

    protected Device() {}

    public Device(String serialNumber, String name, UUID spaceId) {
        this.serialNumber = serialNumber;
        this.name = name;
        this.status = DeviceStatus.OFFLINE;
        this.spaceId = spaceId;
    }

    public void updateStatus(DeviceStatus status) {
        this.status = status;
    }

    public void updateConfiguration(Map<String, String> configuration) {
        this.configuration = new HashMap<>(configuration);
    }

    public UUID getId() { return id; }
    public String getSerialNumber() { return serialNumber; }
    public String getName() { return name; }
    public DeviceStatus getStatus() { return status; }
    public UUID getSpaceId() { return spaceId; }
    public Map<String, String> getConfiguration() { return new HashMap<>(configuration); }
    public DeviceAudit getAuditFields() { return auditFields; }

    @Embeddable
    public static class DeviceAudit extends AuditableModel {}
}