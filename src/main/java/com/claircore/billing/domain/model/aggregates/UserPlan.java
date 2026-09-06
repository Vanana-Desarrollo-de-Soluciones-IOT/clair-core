package com.claircore.billing.domain.model.aggregates;

import com.claircore.billing.domain.model.valueobjects.PlanType;
import com.claircore.billing.domain.model.valueobjects.UserId;
import com.claircore.shared.domain.model.entities.AuditableModel;
import jakarta.persistence.*;
import com.claircore.shared.domain.model.aggregates.AbstractDomainAggregateRoot;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDate;
import java.util.UUID;

@Entity
@EntityListeners(AuditingEntityListener.class)
public class UserPlan extends AbstractDomainAggregateRoot<UserPlan> {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Embedded
    private UserId userId;

    @Enumerated(EnumType.STRING)
    private PlanType planType;

    private LocalDate startDate;
    private LocalDate endDate;

    @Embedded
    private UserPlanAudit auditFields = new UserPlanAudit();

    protected UserPlan() {}

    public UserPlan(UserId userId) {
        this.userId = userId;
        this.planType = PlanType.FREEMIUM;
        this.startDate = LocalDate.now();
        this.endDate = null;
    }

    public void upgradeToPremium() {
        this.planType = PlanType.PREMIUM;
        this.startDate = LocalDate.now();
        // Premium lasts 30 days
        this.endDate = LocalDate.now().plusDays(30);
    }

    public void downgradeToFreemium() {
        this.planType = PlanType.FREEMIUM;
        this.endDate = null;
    }

    public boolean isPremiumExpired() {
        return this.planType == PlanType.PREMIUM && this.endDate != null && LocalDate.now().isAfter(this.endDate);
    }

    // Getters
    public UUID getId() { return id; }
    public UserId getUserId() { return userId; }
    public PlanType getPlanType() { return planType; }
    public LocalDate getStartDate() { return startDate; }
    public LocalDate getEndDate() { return endDate; }

    @Embeddable
    public static class UserPlanAudit extends AuditableModel {}
}
