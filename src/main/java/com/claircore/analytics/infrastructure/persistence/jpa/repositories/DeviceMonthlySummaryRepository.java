package com.claircore.analytics.infrastructure.persistence.jpa.repositories;

import com.claircore.analytics.domain.model.entities.DeviceMonthlySummary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DeviceMonthlySummaryRepository extends JpaRepository<DeviceMonthlySummary, UUID> {

    @Query("SELECT s FROM DeviceMonthlySummary s WHERE s.deviceId.value = :deviceId AND s.summaryMonth = :month")
    Optional<DeviceMonthlySummary> findByDeviceIdAndMonth(
            @Param("deviceId") UUID deviceId,
            @Param("month") LocalDate month
    );

    @Query("SELECT COUNT(s) > 0 FROM DeviceMonthlySummary s WHERE s.deviceId.value = :deviceId AND s.summaryMonth = :month")
    boolean existsByDeviceIdAndMonth(
            @Param("deviceId") UUID deviceId,
            @Param("month") LocalDate month
    );
}
