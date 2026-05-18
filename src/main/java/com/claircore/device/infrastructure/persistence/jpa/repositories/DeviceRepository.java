package com.claircore.device.infrastructure.persistence.jpa.repositories;

import com.claircore.device.domain.model.entities.Device;
import com.claircore.device.domain.model.valueobjects.DeviceStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface DeviceRepository extends JpaRepository<Device, UUID> {
    interface ProvisionedDeviceProjection {
        UUID getDeviceId();
        String getHardwareId();
        String getApiKey();
        DeviceStatus getStatus();
    }

    Optional<Device> findBySerialNumber(String serialNumber);

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
                   COALESCE(a.status, com.claircore.device.domain.model.valueobjects.DeviceStatus.OFFLINE) as status
            FROM Device d
            LEFT JOIN DeviceAssignment a ON a.device.id = d.id
            """)
    Page<ProvisionedDeviceProjection> findProvisionedDevices(Pageable pageable);
}
