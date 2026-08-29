package com.claircore.device.infrastructure.persistence.jpa.repositories;

import com.claircore.device.domain.model.entities.DeviceAssignment;
import com.claircore.device.domain.model.valueobjects.UserId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DeviceAssignmentRepository extends JpaRepository<DeviceAssignment, UUID> {
    Page<DeviceAssignment> findBySpaceId(UUID spaceId, Pageable pageable);
    long countBySpaceId(UUID spaceId);
    boolean existsBySpaceId(UUID spaceId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM DeviceAssignment a WHERE a.device.id = :deviceId")
    Optional<DeviceAssignment> findByDeviceId(@Param("deviceId") UUID deviceId);

    @Query("SELECT a FROM DeviceAssignment a WHERE a.claimToken.value = :claimToken")
    Optional<DeviceAssignment> findByClaimToken(@Param("claimToken") String claimToken);

    @Query("SELECT CASE WHEN COUNT(a) > 0 THEN true ELSE false END FROM DeviceAssignment a WHERE a.spaceId IN (SELECT s.id FROM Space s WHERE s.organizationId = :organizationId)")
    boolean existsByOrganizationId(@Param("organizationId") UUID organizationId);

    @Query("SELECT COUNT(a) FROM DeviceAssignment a WHERE a.spaceId IN (SELECT s.id FROM Space s WHERE s.ownerUserId = :ownerUserId)")
    long countByOwnerUserId(@Param("ownerUserId") UserId ownerUserId);

    @Query("SELECT a.device.id FROM DeviceAssignment a WHERE a.ownerUserId = :ownerUserId")
    List<UUID> findDeviceIdsByOwnerUserId(@Param("ownerUserId") UserId ownerUserId);

    @Query("SELECT CASE WHEN COUNT(a) > 0 THEN true ELSE false END FROM DeviceAssignment a WHERE a.device.id = :deviceId AND a.ownerUserId = :ownerUserId")
    boolean existsByDeviceIdAndOwnerUserId(@Param("deviceId") UUID deviceId, @Param("ownerUserId") UserId ownerUserId);
}
