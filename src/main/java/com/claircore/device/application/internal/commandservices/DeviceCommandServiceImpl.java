package com.claircore.device.application.internal.commandservices;

import com.claircore.device.application.internal.outboundservices.acl.ExternalBillingService;
import com.claircore.device.application.internal.outboundservices.webhooks.DeviceWebhookNotifier;
import com.claircore.device.domain.model.commands.*;
import com.claircore.device.domain.model.entities.Device;
import com.claircore.device.domain.model.entities.Organization;
import com.claircore.device.domain.model.entities.Space;
import com.claircore.device.domain.model.valueobjects.*;
import com.claircore.device.domain.services.DeviceCommandService;
import com.claircore.device.infrastructure.persistence.jpa.repositories.DeviceRepository;
import com.claircore.device.infrastructure.persistence.jpa.repositories.OrganizationRepository;
import com.claircore.device.infrastructure.persistence.jpa.repositories.SpaceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class DeviceCommandServiceImpl implements DeviceCommandService {

    private final DeviceRepository deviceRepository;
    private final SpaceRepository spaceRepository;
    private final OrganizationRepository organizationRepository;
    private final ExternalBillingService externalBillingService;
    private final DeviceWebhookNotifier deviceWebhookNotifier;

    public DeviceCommandServiceImpl(
            DeviceRepository deviceRepository,
            SpaceRepository spaceRepository,
            OrganizationRepository organizationRepository,
            ExternalBillingService externalBillingService,
            DeviceWebhookNotifier deviceWebhookNotifier) {
        this.deviceRepository = deviceRepository;
        this.spaceRepository = spaceRepository;
        this.organizationRepository = organizationRepository;
        this.externalBillingService = externalBillingService;
        this.deviceWebhookNotifier = deviceWebhookNotifier;
    }

    @Override
    @Transactional
    public List<Device> handle(SeedDevicesCommand command) {
        List<Device> seeded = new ArrayList<>();
        for (int i = 1; i <= command.count(); i++) {
            String serialNumber = "SN-" + String.format("%04d", i);
            if (deviceRepository.findBySerialNumber(serialNumber).isPresent()) {
                continue;
            }
            String hardwareId = "HW-" + String.format("%04d", i);
            if (deviceRepository.existsByHardwareId(hardwareId)) {
                continue;
            }

            Device device = new Device(
                serialNumber,
                "Sensor " + i,
                null,
                new HardwareId(hardwareId),
                ApiKey.generate(),
                new DeviceType("air-quality-v1"),
                ClaimToken.generate()
            );
            Device savedDevice = deviceRepository.save(device);
            seeded.add(savedDevice);
            deviceWebhookNotifier.notifyDeviceChanged(savedDevice);
        }
        return seeded;
    }

    @Override
    @Transactional
    public Device handle(PairDeviceCommand command) {
        Optional<Device> existing = deviceRepository.findByHardwareId(command.hardwareId());
        if (existing.isPresent()) {
            Device device = existing.get();
            if (device.getClaimToken() == null) {
                throw new IllegalStateException("Device already paired");
            }
            // Re-issue claim token if still pending
            deviceWebhookNotifier.notifyDeviceChanged(device);
            return device;
        }

        // If not pre-seeded, create on-the-fly (for flexibility, but you can enforce pre-seeding only)
        String serialNumber = "SN-" + command.hardwareId().substring(command.hardwareId().length() - 4);
        if (deviceRepository.findBySerialNumber(serialNumber).isPresent()) {
            throw new IllegalArgumentException("Serial number collision");
        }

        Device device = new Device(
            serialNumber,
            "Unnamed Sensor",
            null,
            new HardwareId(command.hardwareId()),
            ApiKey.generate(),
            new DeviceType(command.deviceType()),
            ClaimToken.generate()
        );

        Device savedDevice = deviceRepository.save(device);
        deviceWebhookNotifier.notifyDeviceChanged(savedDevice);
        return savedDevice;
    }

    @Override
    @Transactional
    public void handle(DeleteDeviceCommand command) {
        Device device = deviceRepository
            .findById(command.deviceId())
            .orElseThrow(() -> new IllegalArgumentException("Device not found"));

        deviceRepository.delete(device);
        deviceWebhookNotifier.notifyDeviceDeleted(device);
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
    public Optional<Device> findByHardwareId(String hardwareId) {
        return deviceRepository.findByHardwareId(hardwareId);
    }

    @Override
    public Optional<Device> findByApiKey(String apiKey) {
        return deviceRepository.findByApiKey(apiKey);
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
