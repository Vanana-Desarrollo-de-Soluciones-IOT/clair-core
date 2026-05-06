package com.claircore.iam.domain.model.entities;

import com.claircore.iam.domain.model.valueobjects.EmailAddress;
import com.claircore.iam.domain.model.valueobjects.Password;
import com.claircore.iam.domain.model.valueobjects.UserStatus;
import com.claircore.shared.domain.model.entities.AuditableModel;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;

import java.util.UUID;

@Entity
@Table(name = "users")
@JsonIgnoreProperties(ignoreUnknown = true)
public class User extends AuditableModel {

    protected User() {
    }

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Embedded
    @AttributeOverride(name = "address", column = @Column(nullable = false, unique = true))
    private EmailAddress email;

    @Embedded
    private Password password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserStatus status;

    public User(EmailAddress email, Password password) {
        this.email = email;
        this.password = password;
        this.status = UserStatus.PENDING_VERIFICATION;
    }

    public void activate() {
        this.status = UserStatus.ACTIVE;
    }

    @JsonIgnore
    public boolean isActive() {
        return this.status == UserStatus.ACTIVE;
    }

    public UUID getId() {
        return id;
    }

    public EmailAddress getEmail() {
        return email;
    }

    public Password getPassword() {
        return password;
    }

    public UserStatus getStatus() {
        return status;
    }
}
