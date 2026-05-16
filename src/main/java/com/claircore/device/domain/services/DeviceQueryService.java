package com.claircore.device.domain.services;

import com.claircore.device.domain.model.entities.Device;
import com.claircore.device.domain.model.entities.Space;
import com.claircore.device.domain.model.queries.*;
import com.claircore.device.domain.model.valueobjects.UserId;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DeviceQueryService {
    Optional<Space> handle(GetSpaceByIdQuery query);
    List<Space> handle(GetSpacesByOwnerQuery query);
    Optional<Device> handle(GetDeviceByIdQuery query);
    Optional<Device> handle(GetDeviceBySerialNumberQuery query);
    Page<Device> handle(GetDevicesBySpaceQuery query);
    List<Device> findBySpaceId(UUID spaceId);
    Optional<Device> findById(UUID id);
}