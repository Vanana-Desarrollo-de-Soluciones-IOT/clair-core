package com.claircore.device.domain.model.entities;

import com.claircore.device.domain.model.valueobjects.*;
import com.claircore.shared.domain.model.entities.AuditableModel;
import jakarta.persistence.*;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
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

    @Column(name = "space_id")
    private UUID spaceId;

    @ElementCollection
    @CollectionTable(name = "device_configuration", joinColumns = @JoinColumn(name = "device_id"))
    @MapKeyColumn(name = "config_key")
    @Column(name = "config_value")
    private Map<String, String> configuration = new HashMap<>();

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
    @AttributeOverride(name = "value", column = @Column(name = "claim_token", unique = true))
    private ClaimToken claimToken;

    @Column(name = "activated_at")
    private Instant activatedAt;

    @Column(name = "last_seen_at")
    private Instant lastSeenAt;

    @Embedded
    private DeviceAudit auditFields = new DeviceAudit();

    protected Device() {}

    public Device(String serialNumber, String name, UUID spaceId,
                  HardwareId hardwareId, ApiKey apiKey, DeviceType deviceType, ClaimToken claimToken) {
        this.serialNumber = serialNumber;
        this.name = name;
        this.status = DeviceStatus.OFFLINE;
        this.spaceId = spaceId;
        this.hardwareId = hardwareId;
        this.apiKey = apiKey;
        this.deviceType = deviceType;
        this.claimToken = claimToken;
    }

    public void updateStatus(DeviceStatus status) {
        this.status = status;
    }

    public void activate() {
        if (this.activatedAt == null) {
            this.activatedAt = Instant.now();
        }
    }

    public void markLastSeen() {
        this.lastSeenAt = Instant.now();
    }

    public void consumeClaimToken() {
        this.claimToken = null;
    }

    public void claimToSpace(UUID spaceId) {
        if (this.claimToken == null) {
            throw new IllegalStateException("Device already claimed");
        }
        if (spaceId == null) {
            throw new IllegalArgumentException("Space ID must not be null");
        }

        this.spaceId = spaceId;
        this.consumeClaimToken();
        this.activate();
    }

    public UUID getId() { return id; }
    public String getSerialNumber() { return serialNumber; }
    public String getName() { return name; }
    public DeviceStatus getStatus() { return status; }
    public UUID getSpaceId() { return spaceId; }
    public Map<String, String> getConfiguration() { return new HashMap<>(configuration); }
    public HardwareId getHardwareId() { return hardwareId; }
    public ApiKey getApiKey() { return apiKey; }
    public DeviceType getDeviceType() { return deviceType; }
    public ClaimToken getClaimToken() { return claimToken; }
    public Instant getActivatedAt() { return activatedAt; }
    public Instant getLastSeenAt() { return lastSeenAt; }
    public DeviceAudit getAuditFields() { return auditFields; }

    @Embeddable
    public static class DeviceAudit extends AuditableModel {}
}
