package com.claircore.device.domain.model.aggregates;

import com.claircore.device.domain.model.valueobjects.ClaimToken;
import com.claircore.device.domain.model.valueobjects.DeviceStatus;
import com.claircore.device.domain.model.valueobjects.UserId;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Where a device sits and who owns it, plus whatever configuration was pushed to it.
 *
 * <p>It refers to the device by id rather than by object. The previous {@code @ManyToOne(LAZY)}
 * meant reading {@code getDevice().getHardwareId()} outside a transaction depended on open-in-view;
 * an id cannot be lazy.
 */
public class DeviceAssignment {

    private final UUID id;
    private final UUID deviceId;
    private UserId ownerUserId;
    private UUID spaceId;
    private DeviceStatus status;
    private final Map<String, String> configuration;
    private ClaimToken claimToken;
    private Instant activatedAt;
    private Instant lastSeenAt;
    private final Instant createdAt;
    private final Instant updatedAt;

    private DeviceAssignment(UUID id, UUID deviceId, UserId ownerUserId, UUID spaceId, DeviceStatus status,
                             Map<String, String> configuration, ClaimToken claimToken, Instant activatedAt,
                             Instant lastSeenAt, Instant createdAt, Instant updatedAt) {
        if (id == null) {
            throw new IllegalArgumentException("Id must not be null");
        }
        if (deviceId == null) {
            throw new IllegalArgumentException("Device ID must not be null");
        }
        this.id = id;
        this.deviceId = deviceId;
        this.ownerUserId = ownerUserId;
        this.spaceId = spaceId;
        this.status = status;
        this.configuration = configuration == null ? new HashMap<>() : new HashMap<>(configuration);
        this.claimToken = claimToken;
        this.activatedAt = activatedAt;
        this.lastSeenAt = lastSeenAt;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public DeviceAssignment(UUID deviceId, ClaimToken claimToken) {
        this(UUID.randomUUID(), deviceId, null, null, DeviceStatus.OFFLINE, new HashMap<>(),
                requireClaimToken(claimToken), null, null, null, null);
    }

    /** Rebuilds an assignment already in storage; only a persistence assembler should call this. */
    public static DeviceAssignment reconstitute(
            UUID id, UUID deviceId, UserId ownerUserId, UUID spaceId, DeviceStatus status,
            Map<String, String> configuration, ClaimToken claimToken, Instant activatedAt,
            Instant lastSeenAt, Instant createdAt, Instant updatedAt) {
        return new DeviceAssignment(id, deviceId, ownerUserId, spaceId, status, configuration,
                claimToken, activatedAt, lastSeenAt, createdAt, updatedAt);
    }

    private static ClaimToken requireClaimToken(ClaimToken claimToken) {
        if (claimToken == null) {
            throw new IllegalArgumentException("Claim token must not be null");
        }
        return claimToken;
    }

    public void claimToSpace(UUID spaceId, UserId userId) {
        if (this.ownerUserId != null) {
            throw new IllegalStateException("Device already claimed");
        }
        if (spaceId == null) {
            throw new IllegalArgumentException("Space ID must not be null");
        }
        if (userId == null) {
            throw new IllegalArgumentException("User ID must not be null");
        }

        this.ownerUserId = userId;
        this.spaceId = spaceId;
        this.claimToken = null;
        if (this.activatedAt == null) {
            this.activatedAt = Instant.now();
        }
    }

    public void markLastSeen() {
        this.lastSeenAt = Instant.now();
    }

    public void markOnline() {
        this.status = DeviceStatus.ONLINE;
        markLastSeen();
    }

    public void markStandby() {
        this.status = DeviceStatus.STANDBY;
        markLastSeen();
    }

    public void markOffline() {
        this.status = DeviceStatus.OFFLINE;
    }

    public void markError() {
        this.status = DeviceStatus.ERROR;
        markLastSeen();
    }

    public void updatePresence(DeviceStatus status, Instant occurredAt) {
        if (status == null) {
            throw new IllegalArgumentException("Device status must not be null");
        }
        this.status = status;
        if (status != DeviceStatus.OFFLINE) {
            this.lastSeenAt = occurredAt != null ? occurredAt : Instant.now();
        }
    }

    public UUID getId() { return id; }
    public UUID getDeviceId() { return deviceId; }
    public UserId getOwnerUserId() { return ownerUserId; }
    public UUID getSpaceId() { return spaceId; }
    public DeviceStatus getStatus() { return status; }
    public Map<String, String> getConfiguration() { return new HashMap<>(configuration); }

    public Optional<String> findConfigurationValue(String key) {
        return Optional.ofNullable(configuration.get(key));
    }

    public void putConfigurationValue(String key, String value) {
        if (key == null || key.isBlank()) throw new IllegalArgumentException("Configuration key must not be blank");
        if (value == null) throw new IllegalArgumentException("Configuration value must not be null");
        configuration.put(key, value);
    }

    public void removeConfigurationValue(String key) {
        if (key == null || key.isBlank()) throw new IllegalArgumentException("Configuration key must not be blank");
        configuration.remove(key);
    }

    public ClaimToken getClaimToken() { return claimToken; }
    public Instant getActivatedAt() { return activatedAt; }
    public Instant getLastSeenAt() { return lastSeenAt; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
