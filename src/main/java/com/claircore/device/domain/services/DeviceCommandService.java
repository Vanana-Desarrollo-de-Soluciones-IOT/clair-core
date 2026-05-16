package com.claircore.device.domain.services;

import com.claircore.device.domain.model.commands.DeleteDeviceCommand;
import com.claircore.device.domain.model.commands.RegisterDeviceCommand;
import com.claircore.device.domain.model.commands.UpdateDeviceConfigurationCommand;
import com.claircore.device.domain.model.commands.UpdateDeviceNameCommand;
import com.claircore.device.domain.model.commands.UpdateDeviceSerialNumberCommand;
import com.claircore.device.domain.model.commands.UpdateDeviceStatusCommand;
import com.claircore.device.domain.model.entities.Device;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DeviceCommandService {
    Device handle(RegisterDeviceCommand command);
    void handle(UpdateDeviceStatusCommand command);
    void handle(UpdateDeviceConfigurationCommand command);
    void handle(UpdateDeviceNameCommand command);
    void handle(UpdateDeviceSerialNumberCommand command);
    void handle(DeleteDeviceCommand command);
    Optional<Device> findById(UUID id);
    Optional<Device> findBySerialNumber(String serialNumber);
    List<Device> findBySpaceId(UUID spaceId);
    long countBySpaceId(UUID spaceId);
}