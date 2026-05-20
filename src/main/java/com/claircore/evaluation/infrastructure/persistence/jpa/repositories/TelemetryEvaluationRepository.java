package com.claircore.evaluation.infrastructure.persistence.jpa.repositories;

import com.claircore.evaluation.domain.model.entities.TelemetryEvaluation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface TelemetryEvaluationRepository extends JpaRepository<TelemetryEvaluation, UUID> {

    @Query("SELECT te FROM TelemetryEvaluation te WHERE te.deviceId.value = :deviceId ORDER BY te.recordedAt DESC")
    Page<TelemetryEvaluation> findByDeviceId(@Param("deviceId") UUID deviceId, Pageable pageable);

    @Query("SELECT te FROM TelemetryEvaluation te WHERE te.deviceId.value = :deviceId ORDER BY te.recordedAt DESC")
    List<TelemetryEvaluation> findLatestByDeviceId(@Param("deviceId") UUID deviceId, Pageable pageable);
}
