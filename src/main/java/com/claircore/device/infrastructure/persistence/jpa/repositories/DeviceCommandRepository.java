package com.claircore.device.infrastructure.persistence.jpa.repositories;

import com.claircore.device.domain.model.entities.DeviceCommand;
import com.claircore.device.domain.model.valueobjects.DeviceCommandStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DeviceCommandRepository extends JpaRepository<DeviceCommand, UUID> {
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
    @Query("SELECT c FROM DeviceCommand c WHERE c.status IN :statuses AND c.auditFields.createdAt >= COALESCE(:since, c.auditFields.createdAt) ORDER BY c.auditFields.createdAt ASC")
    List<DeviceCommand> findPendingForEdge(@Param("statuses") List<DeviceCommandStatus> statuses, @Param("since") Instant since, Pageable pageable);

    @Query("SELECT c FROM DeviceCommand c WHERE c.status IN :statuses AND c.device.hardwareId.value = :hardwareId AND c.auditFields.createdAt >= COALESCE(:since, c.auditFields.createdAt) ORDER BY c.auditFields.createdAt ASC")
    List<DeviceCommand> findPendingForEdgeByHardware(@Param("statuses") List<DeviceCommandStatus> statuses, @Param("hardwareId") String hardwareId, @Param("since") Instant since, Pageable pageable);
}
