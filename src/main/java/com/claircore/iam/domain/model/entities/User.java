package com.claircore.iam.domain.model.entities;

import com.claircore.iam.domain.model.valueobjects.EmailAddress;
import com.claircore.iam.domain.model.valueobjects.Password;
import com.claircore.iam.domain.model.valueobjects.Username;
import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.util.Date;

@Entity
@Table(name = "users")
@EntityListeners(AuditingEntityListener.class)
public class User {

    protected User() {
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Embedded
    private Username username;

    @Embedded
    @AttributeOverride(name = "address", column = @Column(name = "email", unique = true))
    private EmailAddress email;

    @Embedded
    private Password password;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private Date createdAt;

    @LastModifiedDate
    @Column(nullable = false)
    private Date updatedAt;

    public User(Username username, EmailAddress email, Password password) {
        this.username = username;
        this.email = email;
        this.password = password;
    }

    public Long getId() {
        return id;
    }

    public Username getUsername() {
        return username;
    }

    public EmailAddress getEmail() {
        return email;
    }

    public Password getPassword() {
        return password;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public Date getUpdatedAt() {
        return updatedAt;
    }
}
