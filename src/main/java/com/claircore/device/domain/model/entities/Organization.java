package com.claircore.device.domain.model.entities;

import com.claircore.device.domain.model.valueobjects.PlanType;
import com.claircore.device.domain.model.valueobjects.UserId;
import com.claircore.shared.domain.model.entities.AuditableModel;
import jakarta.persistence.*;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.util.UUID;

@Entity
@Table(name = "organizations")
@EntityListeners(AuditingEntityListener.class)
public class Organization {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Embedded
    private UserId ownerUserId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PlanType planType;

    @Embedded
    private OrganizationAudit auditFields = new OrganizationAudit();

    protected Organization() {}

    public Organization(String name, UserId ownerUserId, PlanType planType) {
        this.name = name;
        this.ownerUserId = ownerUserId;
        this.planType = planType;
    }

    public int getMaxSpaces() {
        return 5;
    }

    public int getMaxDevices() {
        return 10;
    }

    public UUID getId() { return id; }
    public String getName() { return name; }
    public UserId getOwnerUserId() { return ownerUserId; }
    public PlanType getPlanType() { return planType; }
    public OrganizationAudit getAuditFields() { return auditFields; }

    @Embeddable
    public static class OrganizationAudit extends AuditableModel {}
}