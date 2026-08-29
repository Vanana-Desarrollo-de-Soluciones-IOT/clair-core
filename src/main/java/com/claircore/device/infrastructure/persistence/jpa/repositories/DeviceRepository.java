package com.claircore.device.infrastructure.persistence.jpa.repositories;

import com.claircore.device.domain.model.entities.Device;
import com.claircore.device.domain.model.valueobjects.DeviceStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.Date;

@Repository
public interface DeviceRepository extends JpaRepository<Device, UUID> {
    interface ProvisionedDeviceProjection {
        UUID getDeviceId();
        String getHardwareId();
        String getApiKey();
        DeviceStatus getStatus();
        boolean isDeleted();
        Date getUpdatedAt();
    }

    Optional<Device> findBySerialNumber(String serialNumber);

    List<Device> findAllBySerialNumberIn(java.util.Collection<String> serialNumbers);

    @Query("SELECT d FROM Device d WHERE d.hardwareId.value = :hardwareId")
    Optional<Device> findByHardwareId(@Param("hardwareId") String hardwareId);

    @Query("SELECT d FROM Device d WHERE d.apiKey.value = :apiKey")
    Optional<Device> findByApiKey(@Param("apiKey") String apiKey);

    @Query("SELECT CASE WHEN COUNT(d) > 0 THEN true ELSE false END FROM Device d WHERE d.hardwareId.value = :hardwareId")
    boolean existsByHardwareId(@Param("hardwareId") String hardwareId);

    /**
     * Postgres cannot infer the type of a bind parameter that only ever appears in a
     * bare "? IS NULL" check (no other typed context) and rejects the query at parse
     * time with "could not determine data type of parameter $N" -- regardless of what
     * value is actually bound. Rather than special-case that in JPQL, the two cursor
     * parameters are substituted with sentinel values guaranteed to be lower than any
     * real row (epoch for timestamps, the nil UUID for device ids) before delegating to
     * the query below, so every "?" there is always compared against a typed column.
     */
    default Page<ProvisionedDeviceProjection> findProvisionedDevices(Date since, UUID afterId, Pageable pageable) {
        return findProvisionedDevicesForCursor(
                since != null ? since : new Date(0L),
                afterId != null ? afterId : new UUID(0L, 0L),
                pageable);
    }

    @Query("""
            SELECT d.id as deviceId,
                   d.hardwareId.value as hardwareId,
                   d.apiKey.value as apiKey,
                   COALESCE(a.status, com.claircore.device.domain.model.valueobjects.DeviceStatus.OFFLINE) as status,
                   d.deleted as deleted,
                   CASE WHEN a.auditFields.updatedAt IS NOT NULL AND a.auditFields.updatedAt > d.auditFields.updatedAt
                        THEN a.auditFields.updatedAt ELSE d.auditFields.updatedAt END as updatedAt
            FROM Device d
            LEFT JOIN DeviceAssignment a ON a.device.id = d.id
            WHERE (CASE WHEN a.auditFields.updatedAt IS NOT NULL AND a.auditFields.updatedAt > d.auditFields.updatedAt
                        THEN a.auditFields.updatedAt ELSE d.auditFields.updatedAt END > :since) OR
                  ((CASE WHEN a.auditFields.updatedAt IS NOT NULL AND a.auditFields.updatedAt > d.auditFields.updatedAt
                         THEN a.auditFields.updatedAt ELSE d.auditFields.updatedAt END = :since) AND d.id > :afterId)
            ORDER BY CASE WHEN a.auditFields.updatedAt IS NOT NULL AND a.auditFields.updatedAt > d.auditFields.updatedAt
                          THEN a.auditFields.updatedAt ELSE d.auditFields.updatedAt END ASC, d.id ASC
            """)
    Page<ProvisionedDeviceProjection> findProvisionedDevicesForCursor(@Param("since") Date since,
                                                               @Param("afterId") UUID afterId,
                                                               Pageable pageable);
}
