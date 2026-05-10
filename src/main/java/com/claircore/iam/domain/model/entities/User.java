package com.claircore.iam.domain.model.entities;

import com.claircore.iam.domain.model.valueobjects.EmailAddress;
import com.claircore.iam.domain.model.valueobjects.OAuthProvider;
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

    @Enumerated(EnumType.STRING)
    @Column(name = "oauth_provider")
    private OAuthProvider oauthProvider;

    @Column(name = "oauth_subject")
    private String oauthSubject;

    public User(EmailAddress email, Password password) {
        this.email = email;
        this.password = password;
        this.status = UserStatus.PENDING_VERIFICATION;
        this.oauthProvider = OAuthProvider.MAIL;
    }

    public User(EmailAddress email, OAuthProvider oauthProvider, String oauthSubject) {
        this.email = email;
        this.password = generateRandomPassword();
        this.status = UserStatus.ACTIVE;
        this.oauthProvider = oauthProvider;
        this.oauthSubject = oauthSubject;
    }

    public void activate() {
        this.status = UserStatus.ACTIVE;
    }

    public void linkOAuthAccount(OAuthProvider provider, String subject) {
        this.oauthProvider = provider;
        this.oauthSubject = subject;
    }

    @JsonIgnore
    public boolean isActive() {
        return this.status == UserStatus.ACTIVE;
    }

    @JsonIgnore
    public boolean isOAuthUser() {
        return this.oauthProvider != null && this.oauthProvider != OAuthProvider.MAIL;
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

    public OAuthProvider getOauthProvider() {
        return oauthProvider;
    }

    public String getOauthSubject() {
        return oauthSubject;
    }

    private static Password generateRandomPassword() {
        return new Password(UUID.randomUUID().toString() + System.currentTimeMillis());
    }
}
