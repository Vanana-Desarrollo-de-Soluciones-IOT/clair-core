package com.claircore.device.infrastructure.persistence.jpa.repositories;

import com.claircore.device.domain.model.entities.DeviceCommand;
import com.claircore.device.domain.model.valueobjects.DeviceCommandStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DeviceCommandRepository extends JpaRepository<DeviceCommand, UUID> {
    @Query("SELECT c FROM DeviceCommand c WHERE c.device.id = :deviceId AND c.id = :commandId")
    Optional<DeviceCommand> findByDeviceIdAndCommandId(@Param("deviceId") UUID deviceId, @Param("commandId") UUID commandId);

    @Query("SELECT c FROM DeviceCommand c WHERE c.status = :status ORDER BY c.auditFields.createdAt ASC")
    List<DeviceCommand> findByStatusForDispatch(@Param("status") DeviceCommandStatus status, Pageable pageable);
}
