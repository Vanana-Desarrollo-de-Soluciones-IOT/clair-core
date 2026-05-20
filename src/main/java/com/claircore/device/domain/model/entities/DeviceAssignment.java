package com.claircore.device.domain.model.entities;

import com.claircore.device.domain.model.valueobjects.ClaimToken;
import com.claircore.device.domain.model.valueobjects.DeviceStatus;
import com.claircore.device.domain.model.valueobjects.UserId;
import com.claircore.shared.domain.model.entities.AuditableModel;
import jakarta.persistence.*;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "device_assignments")
@EntityListeners(AuditingEntityListener.class)
public class DeviceAssignment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "device_id", nullable = false, unique = true)
    private Device device;

    @Embedded
    @AttributeOverride(name = "userId", column = @Column(name = "owner_user_id"))
    private UserId ownerUserId;

    @Column(name = "space_id")
    private UUID spaceId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DeviceStatus status;

    @ElementCollection
    @CollectionTable(name = "device_assignment_configuration", joinColumns = @JoinColumn(name = "assignment_id"))
    @MapKeyColumn(name = "config_key")
    @Column(name = "config_value")
    private Map<String, String> configuration = new HashMap<>();

    @Embedded
    @AttributeOverride(name = "value", column = @Column(name = "claim_token", unique = true))
    private ClaimToken claimToken;

    @Column(name = "activated_at")
    private Instant activatedAt;

    @Column(name = "last_seen_at")
    private Instant lastSeenAt;

    @Embedded
    private DeviceAssignmentAudit auditFields = new DeviceAssignmentAudit();

    protected DeviceAssignment() {}

    public DeviceAssignment(Device device, ClaimToken claimToken) {
        if (device == null) {
            throw new IllegalArgumentException("Device must not be null");
        }
        if (claimToken == null) {
            throw new IllegalArgumentException("Claim token must not be null");
        }

        this.device = device;
        this.claimToken = claimToken;
        this.status = DeviceStatus.OFFLINE;
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
    public Device getDevice() { return device; }
    public UserId getOwnerUserId() { return ownerUserId; }
    public UUID getSpaceId() { return spaceId; }
    public DeviceStatus getStatus() { return status; }
    public Map<String, String> getConfiguration() { return new HashMap<>(configuration); }
    public ClaimToken getClaimToken() { return claimToken; }
    public Instant getActivatedAt() { return activatedAt; }
    public Instant getLastSeenAt() { return lastSeenAt; }
    public DeviceAssignmentAudit getAuditFields() { return auditFields; }

    @Embeddable
    public static class DeviceAssignmentAudit extends AuditableModel {}
}
