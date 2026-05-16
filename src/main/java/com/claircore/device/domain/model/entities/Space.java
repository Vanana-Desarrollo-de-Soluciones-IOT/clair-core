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

    @Embedded
    private UserId ownerUserId;

    @Embedded
    private SpaceAudit auditFields = new SpaceAudit();

    protected Space() {}

    public Space(String name, UserId ownerUserId) {
        this.name = name;
        this.ownerUserId = ownerUserId;
    }

    public void update(String name) {
        this.name = name;
    }

    public UUID getId() { return id; }
    public String getName() { return name; }
    public UserId getOwnerUserId() { return ownerUserId; }
    public SpaceAudit getAuditFields() { return auditFields; }

    @Embeddable
    public static class SpaceAudit extends AuditableModel {}
}