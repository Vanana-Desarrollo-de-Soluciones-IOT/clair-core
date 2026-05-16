package com.claircore.device.infrastructure.persistence.jpa.repositories;

import com.claircore.device.domain.model.entities.Device;
import com.claircore.device.domain.model.valueobjects.UserId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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

    @Query("SELECT COUNT(d) FROM Device d WHERE d.spaceId IN (SELECT s.id FROM Space s WHERE s.ownerUserId = :ownerUserId)")
    long countByOwnerUserId(@Param("ownerUserId") UserId ownerUserId);
}