package com.claircore.device.domain.services;

import com.claircore.device.domain.model.commands.RemoveDeviceThresholdCommand;
import com.claircore.device.domain.model.commands.UpdateDeviceThresholdCommand;
import com.claircore.device.domain.model.entities.DeviceThreshold;
import com.claircore.device.domain.model.queries.GetDeviceThresholdByMetricQuery;
import com.claircore.device.domain.model.queries.GetDeviceThresholdsByDeviceQuery;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DeviceThresholdCommandService {
    DeviceThreshold handle(UpdateDeviceThresholdCommand command);
    void handle(RemoveDeviceThresholdCommand command);
    Optional<DeviceThreshold> findById(UUID id);
}