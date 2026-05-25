package com.claircore.device.domain.services;

import com.claircore.device.domain.model.entities.Device;
import com.claircore.device.domain.model.entities.DeviceAssignment;
import com.claircore.device.domain.model.entities.Organization;
import com.claircore.device.domain.model.entities.Space;
import com.claircore.device.domain.model.queries.*;
import com.claircore.device.domain.model.valueobjects.UserId;
import org.springframework.data.domain.Page;

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
    boolean isSpaceOwnedByUser(UUID spaceId, UUID userId);
}
