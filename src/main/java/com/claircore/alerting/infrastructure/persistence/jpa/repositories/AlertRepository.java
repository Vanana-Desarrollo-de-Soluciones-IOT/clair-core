package com.claircore.alerting.infrastructure.persistence.jpa.repositories;

import com.claircore.alerting.domain.model.entities.Alert;
import com.claircore.alerting.domain.model.valueobjects.AlertStatus;
import com.claircore.alerting.domain.model.valueobjects.MetricType;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Lock;
import jakarta.persistence.LockModeType;

@Repository
public interface AlertRepository extends JpaRepository<Alert, UUID> {

    @Query("SELECT a FROM Alert a WHERE a.deviceId = :deviceId ORDER BY a.occurredAt DESC")
    Page<Alert> findByDeviceId(@Param("deviceId") UUID deviceId, Pageable pageable);

    @Query("SELECT a FROM Alert a WHERE a.spaceId = :spaceId ORDER BY a.occurredAt DESC")
    Page<Alert> findBySpaceId(@Param("spaceId") UUID spaceId, Pageable pageable);

    @Query("SELECT a FROM Alert a WHERE a.deviceId IN :deviceIds ORDER BY a.occurredAt DESC")
    Page<Alert> findByDeviceIdIn(@Param("deviceIds") Collection<UUID> deviceIds, Pageable pageable);

    interface EdgeAlertProjection { Alert getAlert(); String getHardwareId(); }

    // COALESCE avoids a bare "?  IS NULL" placeholder: Postgres cannot infer that
    // parameter's type (no typed context to unify with) and rejects the query with
    // "could not determine data type of parameter $1". Comparing against the column
    // itself when :since is null keeps the original "no filter" semantics.
    @Query("SELECT a as alert, d.hardwareId.value as hardwareId FROM Alert a JOIN Device d ON d.id = a.deviceId WHERE a.status IN :statuses AND a.occurredAt >= COALESCE(:since, a.occurredAt) ORDER BY a.occurredAt ASC")
    List<EdgeAlertProjection> findPendingForEdge(
            @Param("statuses") Collection<AlertStatus> statuses,
            @Param("since") Instant since,
            Pageable pageable
    );

    @Query("SELECT d.hardwareId.value FROM Alert a JOIN Device d ON d.id = a.deviceId WHERE a.id = :alertId")
    Optional<String> findHardwareIdByAlertId(@Param("alertId") UUID alertId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM Alert a WHERE a.id = :alertId")
    Optional<Alert> findByIdForAcknowledgement(@Param("alertId") UUID alertId);

    Optional<Alert> findFirstByDeviceIdAndMetricAndStatus(UUID deviceId, MetricType metric, AlertStatus status);

    Optional<Alert> findFirstByDeviceIdAndMetricAndStatusIn(UUID deviceId, MetricType metric, Collection<AlertStatus> statuses);

    List<Alert> findByDeviceIdAndStatus(UUID deviceId, AlertStatus status);

    @Query("SELECT a FROM Alert a WHERE a.deviceId = :deviceId AND a.status IN :statuses ORDER BY a.occurredAt DESC")
    Page<Alert> findByDeviceIdAndStatusIn(@Param("deviceId") UUID deviceId, @Param("statuses") Collection<AlertStatus> statuses, Pageable pageable);

    @Query("SELECT a FROM Alert a WHERE a.spaceId = :spaceId AND a.status IN :statuses ORDER BY a.occurredAt DESC")
    Page<Alert> findBySpaceIdAndStatusIn(@Param("spaceId") UUID spaceId, @Param("statuses") Collection<AlertStatus> statuses, Pageable pageable);

    @Query("SELECT a FROM Alert a WHERE a.deviceId IN :deviceIds AND a.status IN :statuses ORDER BY a.occurredAt DESC")
    Page<Alert> findByDeviceIdInAndStatusIn(@Param("deviceIds") Collection<UUID> deviceIds, @Param("statuses") Collection<AlertStatus> statuses, Pageable pageable);

    @Query("SELECT cast(a.occurredAt as java.time.LocalDate), count(a) FROM Alert a WHERE a.spaceId = :spaceId AND a.occurredAt >= :since GROUP BY cast(a.occurredAt as java.time.LocalDate) ORDER BY cast(a.occurredAt as java.time.LocalDate)")
    List<Object[]> countAlertsPerDay(@Param("spaceId") UUID spaceId, @Param("since") Instant since);

    @Query("SELECT cast(a.occurredAt as java.time.LocalDate), count(a) FROM Alert a WHERE a.deviceId IN :deviceIds AND a.occurredAt >= :since GROUP BY cast(a.occurredAt as java.time.LocalDate) ORDER BY cast(a.occurredAt as java.time.LocalDate)")
    List<Object[]> countAlertsPerDayByDeviceIds(@Param("deviceIds") Collection<UUID> deviceIds, @Param("since") Instant since);
}
