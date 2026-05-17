package com.claircore.device.domain.services;

import com.claircore.device.domain.model.commands.DeleteDeviceCommand;
import com.claircore.device.domain.model.commands.ClaimDeviceCommand;
import com.claircore.device.domain.model.commands.PairDeviceCommand;
import com.claircore.device.domain.model.commands.SeedDevicesCommand;
import com.claircore.device.domain.model.entities.Device;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DeviceCommandService {
    List<Device> handle(SeedDevicesCommand command);
    Device handle(PairDeviceCommand command);
    Device handle(ClaimDeviceCommand command);
    void handle(DeleteDeviceCommand command);
    Optional<Device> findById(UUID id);
    Optional<Device> findBySerialNumber(String serialNumber);
    Optional<Device> findByHardwareId(String hardwareId);
    Optional<Device> findByApiKey(String apiKey);
    List<Device> findBySpaceId(UUID spaceId);
    long countBySpaceId(UUID spaceId);
}
