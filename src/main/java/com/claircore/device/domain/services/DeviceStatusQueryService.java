package com.claircore.device.domain.services;

import com.claircore.device.domain.model.entities.DeviceAssignment;
import com.claircore.device.domain.model.queries.GetDeviceStatusByDeviceIdForUserQuery;

import java.util.Optional;

public interface DeviceStatusQueryService {
    Optional<DeviceAssignment> handle(GetDeviceStatusByDeviceIdForUserQuery query);
}

