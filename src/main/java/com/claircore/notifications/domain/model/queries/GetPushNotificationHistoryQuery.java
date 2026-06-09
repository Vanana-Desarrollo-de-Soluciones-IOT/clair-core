package com.claircore.notifications.domain.model.queries;

import org.springframework.data.domain.Pageable;

import java.util.UUID;

public record GetPushNotificationHistoryQuery(UUID userId, Pageable pageable) {
    public GetPushNotificationHistoryQuery {
        if (userId == null) {
            throw new IllegalArgumentException("User ID is required");
        }
        if (pageable == null) {
            throw new IllegalArgumentException("Pageable is required");
        }
    }
}
