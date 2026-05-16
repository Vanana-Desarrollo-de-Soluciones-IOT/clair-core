package com.claircore.device.application.internal.commandservices;

import com.claircore.device.domain.model.commands.DeleteDeviceCommand;
import com.claircore.device.domain.model.commands.RegisterDeviceCommand;
import com.claircore.device.domain.model.commands.UpdateDeviceConfigurationCommand;
import com.claircore.device.domain.model.commands.UpdateDeviceStatusCommand;
import com.claircore.device.domain.model.entities.Device;
import com.claircore.device.domain.services.DeviceCommandService;
import com.claircore.device.infrastructure.persistence.jpa.repositories.DeviceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class DeviceCommandServiceImpl implements DeviceCommandService {

    private final DeviceRepository deviceRepository;

    public DeviceCommandServiceImpl(DeviceRepository deviceRepository) {
        this.deviceRepository = deviceRepository;
    }

    @Override
    @Transactional
    public Device handle(RegisterDeviceCommand command) {
        if (deviceRepository.findBySerialNumber(command.serialNumber()).isPresent()) {
            throw new IllegalArgumentException("Device with serial number already exists");
        }

        Device device = new Device(
            command.serialNumber(),
            command.name(),
            command.spaceId()
        );

        return deviceRepository.save(device);
    }

    @Override
    @Transactional
    public void handle(UpdateDeviceStatusCommand command) {
        Device device = deviceRepository
            .findById(command.deviceId())
            .orElseThrow(() -> new IllegalArgumentException("Device not found"));

        device.updateStatus(command.status());
        deviceRepository.save(device);
    }

    @Override
    @Transactional
    public void handle(UpdateDeviceConfigurationCommand command) {
        Device device = deviceRepository
            .findById(command.deviceId())
            .orElseThrow(() -> new IllegalArgumentException("Device not found"));

        device.updateConfiguration(command.configuration());
        deviceRepository.save(device);
    }

    @Override
    @Transactional
    public void handle(DeleteDeviceCommand command) {
        Device device = deviceRepository
            .findById(command.deviceId())
            .orElseThrow(() -> new IllegalArgumentException("Device not found"));

        deviceRepository.delete(device);
    }

    @Override
    public Optional<Device> findById(UUID id) {
        return deviceRepository.findById(id);
    }

    @Override
    public Optional<Device> findBySerialNumber(String serialNumber) {
        return deviceRepository.findBySerialNumber(serialNumber);
    }

    @Override
    public List<Device> findBySpaceId(UUID spaceId) {
        return deviceRepository.findBySpaceId(spaceId);
    }
}