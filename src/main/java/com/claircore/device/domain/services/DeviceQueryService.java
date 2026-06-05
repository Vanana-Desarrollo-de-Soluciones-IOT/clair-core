package com.claircore.device.domain.services;

import com.claircore.device.domain.model.entities.Device;
import com.claircore.device.domain.model.entities.DeviceAssignment;
import com.claircore.device.domain.model.entities.Organization;
import com.claircore.device.domain.model.entities.Space;
import com.claircore.device.domain.model.queries.*;
import com.claircore.device.domain.model.valueobjects.UserId;
import org.springframework.data.domain.Page;

import java.util.Map;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DeviceQueryService {
    Optional<Organization> handle(GetOrganizationByIdQuery query);
    List<Organization> handle(GetOrganizationsByOwnerQuery query);
    Optional<Space> handle(GetSpaceByIdQuery query);
    List<Space> handle(GetSpacesByOrganizationQuery query);
    Optional<Device> handle(GetDeviceByIdQuery query);
    Optional<Device> handle(GetDeviceBySerialNumberQuery query);
    Optional<Device> handle(GetDeviceByHardwareIdQuery query);
    Optional<Device> handle(GetDeviceByApiKeyQuery query);
    Page<DeviceAssignment> handle(GetDevicesBySpaceQuery query);
    List<Device> handle(GetProvisionedDevicesQuery query);
    Optional<UUID> findSpaceIdByDeviceId(UUID deviceId);
    boolean isDeviceOwnedByUser(UUID deviceId, UUID userId);
    Optional<UUID> findOwnerIdByDeviceId(UUID deviceId);
    boolean isSpaceOwnedByUser(UUID spaceId, UUID userId);
    List<UUID> findDeviceIdsByOwnerId(UUID ownerUserId);
    Optional<DeviceAssignment> findAssignmentByDeviceId(UUID deviceId);

    /**
     * Batch lookup to avoid N+1 queries in read models.
     */
    Map<UUID, String> findDeviceNamesByDeviceIds(List<UUID> deviceIds);

    /**
     * Batch lookup to avoid N+1 queries in read models.
     */
    Map<UUID, String> findSpaceNamesBySpaceIds(List<UUID> spaceIds);
}
