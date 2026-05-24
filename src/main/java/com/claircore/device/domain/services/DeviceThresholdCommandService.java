package com.claircore.device.domain.services;

import com.claircore.device.domain.model.commands.RemoveDeviceThresholdCommand;
import com.claircore.device.domain.model.commands.WriteDeviceThresholdCommand;
import com.claircore.device.domain.model.entities.DeviceThreshold;

import java.util.Optional;
import java.util.UUID;

public interface DeviceThresholdCommandService {
    DeviceThreshold handle(WriteDeviceThresholdCommand command);
    void handle(RemoveDeviceThresholdCommand command);
    Optional<DeviceThreshold> findById(UUID id);
}
