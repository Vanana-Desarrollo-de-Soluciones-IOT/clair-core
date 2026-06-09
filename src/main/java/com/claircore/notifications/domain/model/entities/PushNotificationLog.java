package com.claircore.notifications.domain.model.entities;

import com.claircore.shared.domain.model.entities.AuditableModel;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.UuidGenerator;

import java.util.UUID;

@Entity
@Table(name = "push_notification_logs")
public class PushNotificationLog extends AuditableModel {

    protected PushNotificationLog() {}

    @Id
    @UuidGenerator
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "alert_id")
    private UUID alertId;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String message;

    @Column(nullable = false)
    private boolean sent;

    @Column(name = "error_message")
    private String errorMessage;

    public PushNotificationLog(UUID userId, UUID alertId, String title, String message, boolean sent, String errorMessage) {
        if (userId == null) throw new IllegalArgumentException("User ID is required");
        if (title == null || title.isBlank()) throw new IllegalArgumentException("Title is required");
        if (message == null || message.isBlank()) throw new IllegalArgumentException("Message is required");
        this.userId = userId;
        this.alertId = alertId;
        this.title = title;
        this.message = message;
        this.sent = sent;
        this.errorMessage = errorMessage;
    }

    public static PushNotificationLog sent(UUID userId, UUID alertId, String title, String message) {
        return new PushNotificationLog(userId, alertId, title, message, true, null);
    }

    public static PushNotificationLog failed(UUID userId, UUID alertId, String title, String message, String errorMessage) {
        return new PushNotificationLog(userId, alertId, title, message, false, errorMessage);
    }

    public UUID getId() { return id; }
    public UUID getUserId() { return userId; }
    public UUID getAlertId() { return alertId; }
    public String getTitle() { return title; }
    public String getMessage() { return message; }
    public boolean isSent() { return sent; }
    public String getErrorMessage() { return errorMessage; }
}
