package com.claircore.notifications.domain.model.entities;

import com.claircore.notifications.domain.model.valueobjects.EmailContent;
import com.claircore.notifications.domain.model.valueobjects.EmailRecipient;
import com.claircore.notifications.domain.model.valueobjects.EmailSubject;
import com.claircore.shared.domain.model.entities.AuditableModel;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "email_logs")
public class EmailLog extends AuditableModel {

    protected EmailLog() {
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Embedded
    private EmailRecipient recipientEmail;

    @Embedded
    private EmailSubject subject;

    @Embedded
    private EmailContent content;

    @Column(nullable = false)
    private boolean sent;

    private String errorMessage;

    public EmailLog(EmailRecipient recipientEmail, EmailSubject subject, EmailContent content, boolean sent, String errorMessage) {
        if (recipientEmail == null) throw new IllegalArgumentException("Recipient email is required");
        if (subject == null) throw new IllegalArgumentException("Email subject is required");
        if (content == null) throw new IllegalArgumentException("Email content is required");
        this.recipientEmail = recipientEmail;
        this.subject = subject;
        this.content = content;
        this.sent = sent;
        this.errorMessage = errorMessage;
    }

    public static EmailLog sent(EmailRecipient recipientEmail, EmailSubject subject, EmailContent content) {
        return new EmailLog(recipientEmail, subject, content, true, null);
    }

    public static EmailLog failed(EmailRecipient recipientEmail, EmailSubject subject, EmailContent content, String errorMessage) {
        return new EmailLog(recipientEmail, subject, content, false, errorMessage);
    }

    public Long getId() {
        return id;
    }

    public String getRecipientEmail() {
        return recipientEmail.address();
    }

    public String getSubject() {
        return subject.value();
    }

    public String getContent() {
        return content.html();
    }

    public boolean isSent() {
        return sent;
    }

    public String getErrorMessage() {
        return errorMessage;
    }
}
