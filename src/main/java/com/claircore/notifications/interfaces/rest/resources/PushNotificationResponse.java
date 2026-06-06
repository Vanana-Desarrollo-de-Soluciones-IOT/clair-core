package com.claircore.notifications.interfaces.rest.resources;

import com.claircore.notifications.domain.model.entities.PushNotificationLog;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;

@Schema(description = "Push notification history item")
public record PushNotificationResponse(
        @Schema(description = "Push notification log ID") UUID id,
        @Schema(description = "Recipient user ID") UUID userId,
        @Schema(description = "Related alert ID", nullable = true) UUID alertId,
        @Schema(description = "Notification title") String title,
        @Schema(description = "Notification message") String message,
        @Schema(description = "Delivery status", example = "SENT") String status,
        @Schema(description = "Delivery error message", nullable = true) String errorMessage,
        @Schema(description = "Creation timestamp") Instant createdAt
) {
    public static PushNotificationResponse from(PushNotificationLog notification) {
        return new PushNotificationResponse(
                notification.getId(),
                notification.getUserId(),
                notification.getAlertId(),
                notification.getTitle(),
                notification.getMessage(),
                notification.isSent() ? "SENT" : "FAILED",
                notification.getErrorMessage(),
                notification.getCreatedAt().toInstant()
        );
    }
}
