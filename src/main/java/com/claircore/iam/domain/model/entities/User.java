package com.claircore.iam.domain.model.entities;

import com.claircore.iam.domain.model.valueobjects.EmailAddress;
import com.claircore.iam.domain.model.valueobjects.Password;
import com.claircore.shared.domain.model.entities.AuditableModel;
import jakarta.persistence.*;

import java.util.UUID;

@Entity
@Table(name = "users")
public class User extends AuditableModel {

    protected User() {
    }

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Embedded
    private EmailAddress email;

    @Embedded
    private Password password;

    public User(EmailAddress email, Password password) {
        this.email = email;
        this.password = password;
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
}
