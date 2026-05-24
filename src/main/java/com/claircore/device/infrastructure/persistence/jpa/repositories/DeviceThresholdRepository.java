package com.claircore.device.infrastructure.persistence.jpa.repositories;

import com.claircore.device.domain.model.entities.DeviceThreshold;
import com.claircore.device.domain.model.valueobjects.MetricThreshold;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DeviceThresholdRepository extends JpaRepository<DeviceThreshold, UUID> {

    @Query("SELECT t FROM DeviceThreshold t WHERE t.assignmentId = :assignmentId")
    List<DeviceThreshold> findByAssignmentId(@Param("assignmentId") UUID assignmentId);

    @Query("SELECT t FROM DeviceThreshold t WHERE t.assignmentId = :assignmentId AND t.metric = :metric")
    Optional<DeviceThreshold> findByAssignmentIdAndMetric(
            @Param("assignmentId") UUID assignmentId,
            @Param("metric") MetricThreshold metric
    );

    @Query("SELECT t FROM DeviceThreshold t WHERE t.assignmentId = :assignmentId AND t.enabled = true")
    List<DeviceThreshold> findEnabledByAssignmentId(@Param("assignmentId") UUID assignmentId);

    @Query("SELECT CASE WHEN COUNT(t) > 0 THEN true ELSE false END FROM DeviceThreshold t WHERE t.id = :thresholdId AND t.assignmentId IN (SELECT a.id FROM DeviceAssignment a WHERE a.device.id = :deviceId)")
    boolean existsByIdAndDeviceId(@Param("thresholdId") UUID thresholdId, @Param("deviceId") UUID deviceId);
}