package com.claircore.notifications.domain.services;

import java.util.UUID;

public interface PushNotificationDeliveryService {
    void sendPushNotification(UUID userId, String title, String message);
}
