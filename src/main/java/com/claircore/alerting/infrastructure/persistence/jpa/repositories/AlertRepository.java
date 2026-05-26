package com.claircore.alerting.infrastructure.persistence.jpa.repositories;

import com.claircore.alerting.domain.model.entities.Alert;
import com.claircore.alerting.domain.model.valueobjects.AlertStatus;
import com.claircore.alerting.domain.model.valueobjects.MetricType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.Collection;
import java.util.UUID;

@Repository
public interface AlertRepository extends JpaRepository<Alert, UUID> {

    @Query("SELECT a FROM Alert a WHERE a.deviceId = :deviceId ORDER BY a.occurredAt DESC")
    Page<Alert> findByDeviceId(@Param("deviceId") UUID deviceId, Pageable pageable);

    @Query("SELECT a FROM Alert a WHERE a.spaceId = :spaceId ORDER BY a.occurredAt DESC")
    Page<Alert> findBySpaceId(@Param("spaceId") UUID spaceId, Pageable pageable);

    Optional<Alert> findFirstByDeviceIdAndMetricAndStatus(UUID deviceId, MetricType metric, AlertStatus status);

    Optional<Alert> findFirstByDeviceIdAndMetricAndStatusIn(UUID deviceId, MetricType metric, Collection<AlertStatus> statuses);
}
