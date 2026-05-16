package com.claircore.device.application.internal.commandservices;

import com.claircore.device.application.internal.outboundservices.acl.ExternalBillingService;
import com.claircore.device.domain.model.commands.DeleteDeviceCommand;
import com.claircore.device.domain.model.commands.RegisterDeviceCommand;
import com.claircore.device.domain.model.commands.UpdateDeviceConfigurationCommand;
import com.claircore.device.domain.model.commands.UpdateDeviceNameCommand;
import com.claircore.device.domain.model.commands.UpdateDeviceSerialNumberCommand;
import com.claircore.device.domain.model.commands.UpdateDeviceStatusCommand;
import com.claircore.device.domain.model.entities.Device;
import com.claircore.device.domain.model.entities.Organization;
import com.claircore.device.domain.model.entities.Space;
import com.claircore.device.domain.services.DeviceCommandService;
import com.claircore.device.infrastructure.persistence.jpa.repositories.DeviceRepository;
import com.claircore.device.infrastructure.persistence.jpa.repositories.OrganizationRepository;
import com.claircore.device.infrastructure.persistence.jpa.repositories.SpaceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class DeviceCommandServiceImpl implements DeviceCommandService {

    private final DeviceRepository deviceRepository;
    private final SpaceRepository spaceRepository;
    private final OrganizationRepository organizationRepository;
    private final ExternalBillingService externalBillingService;

    public DeviceCommandServiceImpl(
            DeviceRepository deviceRepository,
            SpaceRepository spaceRepository,
            OrganizationRepository organizationRepository,
            ExternalBillingService externalBillingService) {
        this.deviceRepository = deviceRepository;
        this.spaceRepository = spaceRepository;
        this.organizationRepository = organizationRepository;
        this.externalBillingService = externalBillingService;
    }

    @Override
    @Transactional
    public Device handle(RegisterDeviceCommand command) {
        if (deviceRepository.findBySerialNumber(command.serialNumber()).isPresent()) {
            throw new IllegalArgumentException("Device with serial number already exists");
        }

        Space space = spaceRepository
            .findById(command.spaceId())
            .orElseThrow(() -> new IllegalArgumentException("Space not found"));

        UUID userId = space.getOwnerUserId().userId();
        long currentCount = deviceRepository.countByOwnerUserId(space.getOwnerUserId());
        int maxAllowed = externalBillingService.getMaxDevices(userId);

        if (currentCount >= maxAllowed) {
            throw new IllegalStateException(
                "Cannot register device. User has " + currentCount + " devices, max allowed is " + maxAllowed
            );
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
    public void handle(UpdateDeviceNameCommand command) {
        Device device = deviceRepository
            .findById(command.deviceId())
            .orElseThrow(() -> new IllegalArgumentException("Device not found"));

        device.updateName(command.name());
        deviceRepository.save(device);
    }

    @Override
    @Transactional
    public void handle(UpdateDeviceSerialNumberCommand command) {
        if (deviceRepository.findBySerialNumber(command.serialNumber()).isPresent()) {
            throw new IllegalArgumentException("Device with serial number already exists");
        }

        Device device = deviceRepository
            .findById(command.deviceId())
            .orElseThrow(() -> new IllegalArgumentException("Device not found"));

        device.updateSerialNumber(command.serialNumber());
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

    @Override
    public long countBySpaceId(UUID spaceId) {
        return deviceRepository.countBySpaceId(spaceId);
    }
}