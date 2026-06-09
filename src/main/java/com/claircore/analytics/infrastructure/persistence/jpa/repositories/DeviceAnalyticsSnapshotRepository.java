package com.claircore.analytics.infrastructure.persistence.jpa.repositories;

import com.claircore.analytics.domain.model.entities.DeviceAnalyticsSnapshot;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface DeviceAnalyticsSnapshotRepository extends JpaRepository<DeviceAnalyticsSnapshot, UUID> {

    @Query("SELECT s FROM DeviceAnalyticsSnapshot s WHERE s.deviceId.value = :deviceId AND s.timeWindowStart >= :start AND s.timeWindowStart < :end ORDER BY s.timeWindowStart ASC")
    List<DeviceAnalyticsSnapshot> findByDeviceIdAndTimeWindowStartBetween(
            @Param("deviceId") UUID deviceId,
            @Param("start") Instant start,
            @Param("end") Instant end,
            Pageable pageable
    );

    @Query("SELECT s FROM DeviceAnalyticsSnapshot s WHERE s.deviceId.value = :deviceId ORDER BY s.timeWindowEnd DESC")
    List<DeviceAnalyticsSnapshot> findLatestByDeviceId(
            @Param("deviceId") UUID deviceId,
            Pageable pageable
    );

    @Query("SELECT s FROM DeviceAnalyticsSnapshot s WHERE s.deviceId.value IN :deviceIds AND s.timeWindowEnd = (SELECT MAX(s2.timeWindowEnd) FROM DeviceAnalyticsSnapshot s2 WHERE s2.deviceId.value = s.deviceId.value)")
    List<DeviceAnalyticsSnapshot> findLatestByDeviceIds(@Param("deviceIds") List<UUID> deviceIds);

    @Query("""
        SELECT 
            AVG(s.averageCo2), 
            AVG(s.averagePm2_5), 
            AVG(s.averageTemperature), 
            AVG(s.averageHumidity) 
        FROM DeviceAnalyticsSnapshot s 
        WHERE s.deviceId.value = :deviceId AND s.timeWindowStart >= :start AND s.timeWindowStart < :end
    """)
    List<Object[]> findAveragesByDeviceIdAndTimeWindow(@Param("deviceId") UUID deviceId, @Param("start") Instant start, @Param("end") Instant end);
}
