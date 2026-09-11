package com.claircore.evaluation.infrastructure.persistence.jpa.repositories;

import com.claircore.evaluation.domain.model.valueobjects.DeviceId;
import com.claircore.evaluation.infrastructure.persistence.jpa.entities.TelemetryEvaluationPersistenceEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface TelemetryEvaluationPersistenceRepository extends JpaRepository<TelemetryEvaluationPersistenceEntity, UUID> {

    Optional<TelemetryEvaluationPersistenceEntity> findByDeviceIdAndReadingId(DeviceId deviceId, UUID readingId);

    Page<TelemetryEvaluationPersistenceEntity> findByDeviceIdOrderByRecordedAtDesc(DeviceId deviceId, Pageable pageable);

    Optional<TelemetryEvaluationPersistenceEntity> findFirstByDeviceIdOrderByRecordedAtDesc(DeviceId deviceId);
    Page<TelemetryEvaluationPersistenceEntity> findByDeviceIdAndRecordedAtGreaterThanEqualOrderByRecordedAtDesc(
            DeviceId deviceId, java.time.Instant since, Pageable pageable);
    Optional<TelemetryEvaluationPersistenceEntity> findFirstByDeviceIdAndRecordedAtGreaterThanEqualOrderByRecordedAtDesc(
            DeviceId deviceId, java.time.Instant since);
}
