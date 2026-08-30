package com.claircore.device.application.internal.commandservices;

import com.claircore.device.application.internal.outboundservices.acl.DeviceChangedIntegrationEvent;
import com.claircore.device.application.internal.outboundservices.acl.ExternalBillingService;
import com.claircore.device.application.internal.outboundservices.acl.ProvisioningDevicesChangedPublisher;
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

import java.time.Instant;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;

@Service
public class DeviceCommandServiceImpl implements DeviceCommandService {

    private static final SecureRandom HARDWARE_ID_RANDOM = new SecureRandom();
    private static final char[] HARDWARE_ID_ALPHABET = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ".toCharArray();
    private static final int HARDWARE_ID_SUFFIX_LENGTH = 4;

    private final DeviceRepository deviceRepository;
    private final DeviceAssignmentRepository deviceAssignmentRepository;
    private final SpaceRepository spaceRepository;
    private final OrganizationRepository organizationRepository;
    private final ExternalBillingService externalBillingService;
    private final ProvisioningDevicesChangedPublisher provisioningDevicesChangedPublisher;

    public DeviceCommandServiceImpl(
            DeviceRepository deviceRepository,
            DeviceAssignmentRepository deviceAssignmentRepository,
            SpaceRepository spaceRepository,
            OrganizationRepository organizationRepository,
            ExternalBillingService externalBillingService,
            ProvisioningDevicesChangedPublisher provisioningDevicesChangedPublisher) {
        this.deviceRepository = deviceRepository;
        this.deviceAssignmentRepository = deviceAssignmentRepository;
        this.spaceRepository = spaceRepository;
        this.organizationRepository = organizationRepository;
        this.externalBillingService = externalBillingService;
        this.provisioningDevicesChangedPublisher = provisioningDevicesChangedPublisher;
    }

    @Override
    @Transactional
    public List<Device> handle(SeedDevicesCommand command) {
        List<String> expectedSerialNumbers = new ArrayList<>();
        for (int i = 1; i <= command.count(); i++) {
            expectedSerialNumbers.add("SN-" + String.format("%04d", i));
        }

        // Optimized: Single query to find all existing devices by serial number.
        List<String> existingSerialNumbers = deviceRepository.findAllBySerialNumberIn(expectedSerialNumbers)
                .stream()
                .map(Device::getSerialNumber)
                .toList();

        List<Device> seeded = new ArrayList<>();
        for (String serialNumber : expectedSerialNumbers) {
            if (existingSerialNumbers.contains(serialNumber)) {
                continue;
            }

            // Factory inventory hardware IDs must be random and unique.
            String hardwareId = generateUniqueHardwareId();

            Device device = new Device(
                serialNumber,
                "Sensor " + serialNumber.substring(3),
                new HardwareId(hardwareId),
                ApiKey.generate(),
                new DeviceType("air-quality-v1")
            );
            Device savedDevice = deviceRepository.save(device);
            // Notify edge so it can reconcile its provisioning cache.
            publishDeviceChanged(savedDevice, DeviceStatus.OFFLINE.name(), "CREATED");
            seeded.add(savedDevice);
        }
        return seeded;
    }

    private String generateUniqueHardwareId() {
        // Very low collision probability, but we still guard uniqueness via the repository.
        for (int attempt = 0; attempt < 50; attempt++) {
            String candidate = "CLAIR-" + randomSuffix();
            if (!deviceRepository.existsByHardwareId(candidate)) {
                return candidate;
            }
        }
        throw new IllegalStateException("Unable to generate a unique hardware id after multiple attempts");
    }

    private String randomSuffix() {
        char[] buf = new char[HARDWARE_ID_SUFFIX_LENGTH];
        for (int i = 0; i < HARDWARE_ID_SUFFIX_LENGTH; i++) {
            buf[i] = HARDWARE_ID_ALPHABET[HARDWARE_ID_RANDOM.nextInt(HARDWARE_ID_ALPHABET.length)];
        }
        return new String(buf);
    }

    @Override
    @Transactional
    public DeviceAssignment handle(PairDeviceCommand command) {
        Device device = deviceRepository
            .findByHardwareId(command.hardwareId())
            .orElseThrow(() -> new IllegalArgumentException("Device not registered in factory inventory"));

        Optional<DeviceAssignment> existingAssignment = deviceAssignmentRepository.findByDeviceIdForUpdate(device.getId());
        if (existingAssignment.isPresent()) {
            DeviceAssignment assignment = existingAssignment.get();
            if (assignment.getOwnerUserId() != null) {
                throw new IllegalStateException("Device already paired");
            }

            publishDeviceChanged(assignment, assignment.getStatus().name());
            return assignment;
        }

        DeviceAssignment assignment = deviceAssignmentRepository.save(new DeviceAssignment(device, ClaimToken.generate()));
        publishDeviceChanged(assignment, DeviceStatus.OFFLINE.name());
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
        publishDeviceChanged(savedAssignment, savedAssignment.getStatus().name());
        return savedAssignment;
    }

    @Override
    @Transactional
    public void handle(ResetDeviceAssignmentCommand command) {
        DeviceAssignment assignment = deviceAssignmentRepository
            .findByDeviceIdForUpdate(command.deviceId())
            .orElseThrow(() -> new IllegalArgumentException("Device assignment not found"));

        if (assignment.getOwnerUserId() == null || !assignment.getOwnerUserId().equals(command.userId())) {
            throw new AccessDeniedException("Device does not belong to user");
        }

        // Reset/unlink is not a decommission. Keep the device active and cached.
        Device device = assignment.getDevice();
        device.resetNameToFactoryDefault();
        deviceRepository.save(device);

        deviceAssignmentRepository.delete(assignment);
        // Reset/unlink is not a decommission. Keep the device cached on the edge.
        publishDeviceChanged(device, DeviceStatus.OFFLINE.name(), "UPDATED");
    }

    @Override
    @Transactional
    public void handle(UpdateDeviceNameCommand command) {
        DeviceAssignment assignment = deviceAssignmentRepository
            .findByDeviceIdForUpdate(command.deviceId())
            .orElseThrow(() -> new IllegalArgumentException("Device assignment not found"));

        if (assignment.getOwnerUserId() == null || !assignment.getOwnerUserId().equals(command.userId())) {
            throw new AccessDeniedException("Device does not belong to user");
        }

        Device device = assignment.getDevice();
        device.updateName(command.name());
        deviceRepository.save(device);

        // Notify downstream consumers with the latest combined view.
        publishDeviceChanged(assignment, assignment.getStatus().name());
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
        // Never run an unbounded query; callers needing more should use the paged query API.
        return deviceAssignmentRepository.findBySpaceId(spaceId, PageRequest.of(0, 1000))
            .map(DeviceAssignment::getDevice)
            .toList();
    }

    @Override
    public long countBySpaceId(UUID spaceId) {
        return deviceAssignmentRepository.countBySpaceId(spaceId);
    }

    private void publishDeviceChanged(DeviceAssignment assignment, String status) {
        Device device = assignment.getDevice();
        provisioningDevicesChangedPublisher.publish(new DeviceChangedIntegrationEvent(
                device.getId().toString(),
                device.getHardwareId().value(),
                device.getApiKey().value(),
                status,
                "UPDATED",
                Instant.now().toString()
        ));
    }

    private void publishDeviceChanged(Device device, String status, String changeType) {
        provisioningDevicesChangedPublisher.publish(new DeviceChangedIntegrationEvent(
                device.getId().toString(),
                device.getHardwareId().value(),
                device.getApiKey().value(),
                status,
                changeType,
                Instant.now().toString()
        ));
    }
}
