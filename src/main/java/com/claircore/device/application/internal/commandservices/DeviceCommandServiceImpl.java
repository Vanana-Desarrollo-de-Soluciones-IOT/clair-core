package com.claircore.device.application.internal.commandservices;

import com.claircore.device.application.internal.outboundservices.acl.ExternalBillingService;
import com.claircore.device.application.internal.outboundservices.webhooks.DeviceWebhookNotifier;
import com.claircore.device.domain.model.commands.*;
import com.claircore.device.domain.model.entities.Device;
import com.claircore.device.domain.model.entities.DeviceAssignment;
import com.claircore.device.domain.model.entities.Organization;
import com.claircore.device.domain.model.entities.Space;
import com.claircore.device.domain.model.valueobjects.*;
import com.claircore.device.domain.services.DeviceCommandService;
import com.claircore.device.infrastructure.persistence.jpa.repositories.DeviceAssignmentRepository;
import com.claircore.device.infrastructure.persistence.jpa.repositories.DeviceRepository;
import com.claircore.device.infrastructure.persistence.jpa.repositories.OrganizationRepository;
import com.claircore.device.infrastructure.persistence.jpa.repositories.SpaceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.access.AccessDeniedException;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class DeviceCommandServiceImpl implements DeviceCommandService {

    private final DeviceRepository deviceRepository;
    private final DeviceAssignmentRepository deviceAssignmentRepository;
    private final SpaceRepository spaceRepository;
    private final OrganizationRepository organizationRepository;
    private final ExternalBillingService externalBillingService;
    private final DeviceWebhookNotifier deviceWebhookNotifier;

    public DeviceCommandServiceImpl(
            DeviceRepository deviceRepository,
            DeviceAssignmentRepository deviceAssignmentRepository,
            SpaceRepository spaceRepository,
            OrganizationRepository organizationRepository,
            ExternalBillingService externalBillingService,
            DeviceWebhookNotifier deviceWebhookNotifier) {
        this.deviceRepository = deviceRepository;
        this.deviceAssignmentRepository = deviceAssignmentRepository;
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
            String hardwareId = "CLAIR-" + String.format("%04d", i);
            if (deviceRepository.existsByHardwareId(hardwareId)) {
                continue;
            }

            Device device = new Device(
                serialNumber,
                "Sensor " + i,
                new HardwareId(hardwareId),
                ApiKey.generate(),
                new DeviceType("air-quality-v1")
            );
            Device savedDevice = deviceRepository.save(device);
            seeded.add(savedDevice);
        }
        return seeded;
    }

    @Override
    @Transactional
    public DeviceAssignment handle(PairDeviceCommand command) {
        Device device = deviceRepository
            .findByHardwareId(command.hardwareId())
            .orElseThrow(() -> new IllegalArgumentException("Device not registered in factory inventory"));

        Optional<DeviceAssignment> existingAssignment = deviceAssignmentRepository.findByDeviceId(device.getId());
        if (existingAssignment.isPresent()) {
            DeviceAssignment assignment = existingAssignment.get();
            if (assignment.getOwnerUserId() != null) {
                throw new IllegalStateException("Device already paired");
            }

            deviceWebhookNotifier.notifyDeviceChanged(assignment);
            return assignment;
        }

        DeviceAssignment assignment = deviceAssignmentRepository.save(new DeviceAssignment(device, ClaimToken.generate()));
        deviceWebhookNotifier.notifyDeviceChanged(assignment);
        return assignment;
    }

    @Override
    @Transactional
    public DeviceAssignment handle(ClaimDeviceCommand command) {
        Space space = spaceRepository
            .findById(command.spaceId())
            .orElseThrow(() -> new IllegalArgumentException("Space not found"));

        if (!space.getOwnerUserId().equals(command.userId())) {
            throw new AccessDeniedException("Space does not belong to user");
        }

        DeviceAssignment assignment = deviceAssignmentRepository
            .findByClaimToken(command.claimToken())
            .orElseThrow(() -> new IllegalArgumentException("Invalid claim token"));

        if (assignment.getOwnerUserId() != null && !assignment.getOwnerUserId().equals(command.userId())) {
            throw new AccessDeniedException("Device assignment belongs to another user");
        }

        assignment.claimToSpace(command.spaceId(), command.userId());
        DeviceAssignment savedAssignment = deviceAssignmentRepository.save(assignment);
        deviceWebhookNotifier.notifyDeviceChanged(savedAssignment);
        return savedAssignment;
    }

    @Override
    @Transactional
    public void handle(ResetDeviceAssignmentCommand command) {
        DeviceAssignment assignment = deviceAssignmentRepository
            .findByDeviceId(command.deviceId())
            .orElseThrow(() -> new IllegalArgumentException("Device assignment not found"));

        if (assignment.getOwnerUserId() == null || !assignment.getOwnerUserId().equals(command.userId())) {
            throw new AccessDeniedException("Device does not belong to user");
        }

        deviceAssignmentRepository.delete(assignment);
        deviceWebhookNotifier.notifyDeviceDeleted(assignment);
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
        return deviceAssignmentRepository.findBySpaceId(spaceId, org.springframework.data.domain.Pageable.unpaged())
            .map(DeviceAssignment::getDevice)
            .toList();
    }

    @Override
    public long countBySpaceId(UUID spaceId) {
        return deviceAssignmentRepository.countBySpaceId(spaceId);
    }
}
