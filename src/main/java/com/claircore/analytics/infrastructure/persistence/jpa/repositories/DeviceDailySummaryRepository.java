package com.claircore.analytics.infrastructure.persistence.jpa.repositories;

import com.claircore.analytics.domain.model.entities.DeviceDailySummary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DeviceDailySummaryRepository extends JpaRepository<DeviceDailySummary, UUID> {

    @Query("SELECT s FROM DeviceDailySummary s WHERE s.deviceId.value = :deviceId AND s.summaryDate = :date")
    Optional<DeviceDailySummary> findByDeviceIdAndDate(
            @Param("deviceId") UUID deviceId,
            @Param("date") LocalDate date
    );

    @Query("SELECT s FROM DeviceDailySummary s WHERE s.deviceId.value = :deviceId ORDER BY s.summaryDate DESC LIMIT 1")
    Optional<DeviceDailySummary> findLatestByDeviceId(@Param("deviceId") UUID deviceId);

    @Query("SELECT s FROM DeviceDailySummary s WHERE s.deviceId.value = :deviceId AND s.summaryDate BETWEEN :start AND :end ORDER BY s.summaryDate ASC")
    List<DeviceDailySummary> findByDeviceIdAndDateBetween(
            @Param("deviceId") UUID deviceId,
            @Param("start") LocalDate start,
            @Param("end") LocalDate end
    );

    @Query("SELECT COUNT(s) > 0 FROM DeviceDailySummary s WHERE s.deviceId.value = :deviceId AND s.summaryDate = :date")
    boolean existsByDeviceIdAndDate(
            @Param("deviceId") UUID deviceId,
            @Param("date") LocalDate date
    );

    @Query("SELECT s FROM DeviceDailySummary s WHERE s.summaryDate BETWEEN :start AND :end ORDER BY s.deviceId.value, s.summaryDate ASC")
    List<DeviceDailySummary> findAllByDateBetween(
            @Param("start") LocalDate start,
            @Param("end") LocalDate end
    );
}
