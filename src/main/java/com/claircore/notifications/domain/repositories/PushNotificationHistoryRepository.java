package com.claircore.notifications.domain.repositories;

import com.claircore.notifications.domain.model.entities.PushNotificationLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface PushNotificationHistoryRepository {
    PushNotificationLog save(PushNotificationLog pushNotificationLog);

    Page<PushNotificationLog> findByUserId(UUID userId, Pageable pageable);
}
