package com.claircore.device.infrastructure.persistence.jpa.repositories;

import com.claircore.device.domain.model.entities.Device;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DeviceRepository extends JpaRepository<Device, UUID> {
    Optional<Device> findBySerialNumber(String serialNumber);
    List<Device> findBySpaceId(UUID spaceId);
    Page<Device> findBySpaceId(UUID spaceId, Pageable pageable);
    long countBySpaceId(UUID spaceId);
    boolean existsBySpaceId(UUID spaceId);
}