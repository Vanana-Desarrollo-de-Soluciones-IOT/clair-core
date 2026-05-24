package com.claircore.device.domain.services;

import com.claircore.device.domain.model.entities.DeviceThreshold;
import com.claircore.device.domain.model.queries.GetDeviceThresholdByMetricQuery;
import com.claircore.device.domain.model.queries.GetDeviceThresholdsByDeviceQuery;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DeviceThresholdQueryService {
    List<DeviceThreshold> handle(GetDeviceThresholdsByDeviceQuery query);
    Optional<DeviceThreshold> handle(GetDeviceThresholdByMetricQuery query);
    List<DeviceThreshold> findByAssignmentId(UUID assignmentId);
    List<DeviceThreshold> findEnabledByAssignmentId(UUID assignmentId);
}