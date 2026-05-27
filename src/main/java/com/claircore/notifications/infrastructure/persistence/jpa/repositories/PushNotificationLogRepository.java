package com.claircore.notifications.infrastructure.persistence.jpa.repositories;

import com.claircore.notifications.domain.model.entities.PushNotificationLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface PushNotificationLogRepository extends JpaRepository<PushNotificationLog, UUID> {
    Page<PushNotificationLog> findByUserId(UUID userId, Pageable pageable);
}

