package com.claircore.device.domain.model.entities;

import com.claircore.device.domain.model.valueobjects.UserId;
import com.claircore.shared.domain.model.entities.AuditableModel;
import jakarta.persistence.*;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.util.UUID;

@Entity
@Table(name = "spaces")
@EntityListeners(AuditingEntityListener.class)
public class Space {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @Embedded
    private UserId ownerUserId;

    @Embedded
    private SpaceAudit auditFields = new SpaceAudit();

    protected Space() {}

    public Space(String name, UUID organizationId, UserId ownerUserId) {
        this.name = name;
        this.organizationId = organizationId;
        this.ownerUserId = ownerUserId;
    }

    public UUID getId() { return id; }
    public String getName() { return name; }
    public UUID getOrganizationId() { return organizationId; }
    public UserId getOwnerUserId() { return ownerUserId; }
    public SpaceAudit getAuditFields() { return auditFields; }

    @Embeddable
    public static class SpaceAudit extends AuditableModel {}
}