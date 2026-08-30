package com.claircore.device.infrastructure.persistence.jpa.repositories;

import com.claircore.device.domain.model.entities.DeviceCommand;
import com.claircore.device.domain.model.valueobjects.DeviceCommandStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Modifying;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DeviceCommandRepository extends JpaRepository<DeviceCommand, UUID> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM DeviceCommand c WHERE c.id = :commandId")
    Optional<DeviceCommand> findByIdForAcknowledgement(@Param("commandId") UUID commandId);
    @Query("SELECT c FROM DeviceCommand c WHERE c.device.id = :deviceId AND c.id = :commandId")
    Optional<DeviceCommand> findByDeviceIdAndCommandId(@Param("deviceId") UUID deviceId, @Param("commandId") UUID commandId);

    @Query("SELECT c FROM DeviceCommand c WHERE c.device.id = :deviceId ORDER BY c.auditFields.createdAt DESC")
    Optional<DeviceCommand> findLatestByDeviceId(@Param("deviceId") UUID deviceId);

    @Query("SELECT c FROM DeviceCommand c WHERE c.status = :status ORDER BY c.auditFields.createdAt ASC")
    List<DeviceCommand> findByStatusForDispatch(@Param("status") DeviceCommandStatus status, Pageable pageable);

    // COALESCE avoids a bare "?  IS NULL" placeholder: Postgres cannot infer that
    // parameter's type (no typed context to unify with) and rejects the query with
    // "could not determine data type of parameter $N". Comparing against the column
    // itself when :since is null keeps the original "no filter" semantics. The edge
    // never sends since, so this ran on every poll.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM DeviceCommand c JOIN FETCH c.device d WHERE ((c.status = com.claircore.device.domain.model.valueobjects.DeviceCommandStatus.PENDING AND c.auditFields.createdAt >= COALESCE(:since, c.auditFields.createdAt)) OR (c.status = com.claircore.device.domain.model.valueobjects.DeviceCommandStatus.SENT AND c.sentAt <= :leaseCutoff)) ORDER BY COALESCE(c.sentAt, c.auditFields.createdAt) ASC")
    List<DeviceCommand> findPendingForEdge(@Param("since") Instant since, @Param("leaseCutoff") Instant leaseCutoff, Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM DeviceCommand c JOIN FETCH c.device d WHERE c.device.hardwareId.value = :hardwareId AND ((c.status = com.claircore.device.domain.model.valueobjects.DeviceCommandStatus.PENDING AND c.auditFields.createdAt >= COALESCE(:since, c.auditFields.createdAt)) OR (c.status = com.claircore.device.domain.model.valueobjects.DeviceCommandStatus.SENT AND c.sentAt <= :leaseCutoff)) ORDER BY COALESCE(c.sentAt, c.auditFields.createdAt) ASC")
    List<DeviceCommand> findPendingForEdgeByHardware(@Param("hardwareId") String hardwareId, @Param("since") Instant since, @Param("leaseCutoff") Instant leaseCutoff, Pageable pageable);

    @Modifying
    @Query("UPDATE DeviceCommand c SET c.status = com.claircore.device.domain.model.valueobjects.DeviceCommandStatus.SENT, c.sentAt = :claimedAt WHERE c.id = :commandId AND (c.status = com.claircore.device.domain.model.valueobjects.DeviceCommandStatus.PENDING OR (c.status = com.claircore.device.domain.model.valueobjects.DeviceCommandStatus.SENT AND c.sentAt <= :leaseCutoff))")
    int claimForEdge(@Param("commandId") UUID commandId, @Param("leaseCutoff") Instant leaseCutoff, @Param("claimedAt") Instant claimedAt);

}
