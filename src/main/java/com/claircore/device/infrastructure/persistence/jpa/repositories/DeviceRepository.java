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
            WHERE (:since IS NULL OR
                   (CASE WHEN a.auditFields.updatedAt IS NOT NULL AND a.auditFields.updatedAt > d.auditFields.updatedAt
                         THEN a.auditFields.updatedAt ELSE d.auditFields.updatedAt END > :since) OR
                   ((CASE WHEN a.auditFields.updatedAt IS NOT NULL AND a.auditFields.updatedAt > d.auditFields.updatedAt
                          THEN a.auditFields.updatedAt ELSE d.auditFields.updatedAt END = :since) AND (:afterId IS NULL OR d.id > :afterId)))
            ORDER BY CASE WHEN a.auditFields.updatedAt IS NOT NULL AND a.auditFields.updatedAt > d.auditFields.updatedAt
                          THEN a.auditFields.updatedAt ELSE d.auditFields.updatedAt END ASC, d.id ASC
            """)
    Page<ProvisionedDeviceProjection> findProvisionedDevices(@Param("since") Date since,
                                                               @Param("afterId") UUID afterId,
                                                               Pageable pageable);
}
