package com.claircore.device.infrastructure.persistence.jpa.repositories;

import com.claircore.device.domain.model.valueobjects.ApiKey;
import com.claircore.device.domain.model.valueobjects.DeviceStatus;
import com.claircore.device.domain.model.valueobjects.HardwareId;
import com.claircore.device.infrastructure.persistence.jpa.entities.DevicePersistenceEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DevicePersistenceRepository extends JpaRepository<DevicePersistenceEntity, UUID> {

    /** The roster row shape; the adapter maps it to the domain read model. */
    interface ProvisionedDeviceProjection {
        UUID getDeviceId();
        String getHardwareId();
        String getApiKey();
        DeviceStatus getStatus();
        boolean isDeleted();
        Instant getUpdatedAt();
    }

    Optional<DevicePersistenceEntity> findBySerialNumber(String serialNumber);

    List<DevicePersistenceEntity> findAllBySerialNumberIn(Collection<String> serialNumbers);

    Optional<DevicePersistenceEntity> findByHardwareId(HardwareId hardwareId);

    Optional<DevicePersistenceEntity> findByApiKey(ApiKey apiKey);

    boolean existsByHardwareId(HardwareId hardwareId);

    /**
     * Keyset page over the roster, ordered by the later of the device's and its assignment's
     * {@code updatedAt}, then by id.
     *
     * <p>Postgres cannot infer the type of a bind parameter that only ever appears in a bare
     * "? IS NULL" check and rejects the query at parse time, whatever is bound. The adapter
     * therefore substitutes sentinels lower than any real row rather than passing nulls, so every
     * placeholder here is compared against a typed column.
     */
    @Query("""
            SELECT d.id as deviceId,
                   d.hardwareId as hardwareId,
                   d.apiKey as apiKey,
                   COALESCE(a.status, com.claircore.device.domain.model.valueobjects.DeviceStatus.OFFLINE) as status,
                   COALESCE(d.deleted, false) as deleted,
                   CASE WHEN a.updatedAt IS NOT NULL AND a.updatedAt > d.updatedAt
                        THEN a.updatedAt ELSE d.updatedAt END as updatedAt
            FROM DevicePersistenceEntity d
            LEFT JOIN DeviceAssignmentPersistenceEntity a ON a.deviceId = d.id
            WHERE (CASE WHEN a.updatedAt IS NOT NULL AND a.updatedAt > d.updatedAt
                        THEN a.updatedAt ELSE d.updatedAt END > :since) OR
                  ((CASE WHEN a.updatedAt IS NOT NULL AND a.updatedAt > d.updatedAt
                         THEN a.updatedAt ELSE d.updatedAt END = :since) AND d.id > :afterId)
            ORDER BY CASE WHEN a.updatedAt IS NOT NULL AND a.updatedAt > d.updatedAt
                          THEN a.updatedAt ELSE d.updatedAt END ASC, d.id ASC
            """)
    Page<ProvisionedDeviceProjection> findProvisionedDevicesForCursor(
            @Param("since") Instant since, @Param("afterId") UUID afterId, Pageable pageable);
}
