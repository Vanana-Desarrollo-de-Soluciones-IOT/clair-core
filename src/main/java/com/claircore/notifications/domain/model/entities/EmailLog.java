package com.claircore.notifications.domain.model.entities;

import com.claircore.shared.domain.model.entities.AuditableModel;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;

@Entity
@Table(name = "email_logs")
public class EmailLog extends AuditableModel {

    protected EmailLog() {
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(nullable = false)
    private String recipientEmail;

    @NotBlank
    @Column(nullable = false)
    private String subject;

    @Column(columnDefinition = "TEXT")
    private String content;

    @Column(nullable = false)
    private boolean sent;

    private String errorMessage;

    public EmailLog(String recipientEmail, String subject, String content, boolean sent) {
        this.recipientEmail = recipientEmail;
        this.subject = subject;
        this.content = content;
        this.sent = sent;
    }

    public Long getId() {
        return id;
    }

    public String getRecipientEmail() {
        return recipientEmail;
    }

    public String getSubject() {
        return subject;
    }

    public String getContent() {
        return content;
    }

    public boolean isSent() {
        return sent;
    }

    public String getErrorMessage() {
        return errorMessage;
    }
}