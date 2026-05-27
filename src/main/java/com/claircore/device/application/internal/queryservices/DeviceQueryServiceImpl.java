package com.claircore.device.application.internal.queryservices;

import com.claircore.device.domain.model.entities.Device;
import com.claircore.device.domain.model.entities.DeviceAssignment;
import com.claircore.device.domain.model.entities.Organization;
import com.claircore.device.domain.model.entities.Space;
import com.claircore.device.domain.model.queries.*;
import com.claircore.device.domain.services.DeviceQueryService;
import com.claircore.device.infrastructure.persistence.jpa.repositories.DeviceAssignmentRepository;
import com.claircore.device.infrastructure.persistence.jpa.repositories.DeviceRepository;
import com.claircore.device.infrastructure.persistence.jpa.repositories.OrganizationRepository;
import com.claircore.device.infrastructure.persistence.jpa.repositories.SpaceRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class DeviceQueryServiceImpl implements DeviceQueryService {

    private final OrganizationRepository organizationRepository;
    private final SpaceRepository spaceRepository;
    private final DeviceRepository deviceRepository;
    private final DeviceAssignmentRepository deviceAssignmentRepository;

    public DeviceQueryServiceImpl(
            OrganizationRepository organizationRepository,
            SpaceRepository spaceRepository,
            DeviceRepository deviceRepository,
            DeviceAssignmentRepository deviceAssignmentRepository) {
        this.organizationRepository = organizationRepository;
        this.spaceRepository = spaceRepository;
        this.deviceRepository = deviceRepository;
        this.deviceAssignmentRepository = deviceAssignmentRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Organization> handle(GetOrganizationByIdQuery query) {
        return organizationRepository.findById(query.organizationId());
    }

    @Override
    @Transactional(readOnly = true)
    public List<Organization> handle(GetOrganizationsByOwnerQuery query) {
        return organizationRepository.findByOwnerUserId(query.ownerUserId());
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Space> handle(GetSpaceByIdQuery query) {
        return spaceRepository.findById(query.spaceId());
    }

    @Override
    @Transactional(readOnly = true)
    public List<Space> handle(GetSpacesByOrganizationQuery query) {
        return spaceRepository.findByOrganizationId(query.organizationId());
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Device> handle(GetDeviceByIdQuery query) {
        return deviceRepository.findById(query.deviceId());
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Device> handle(GetDeviceBySerialNumberQuery query) {
        return deviceRepository.findBySerialNumber(query.serialNumber());
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Device> handle(GetDeviceByHardwareIdQuery query) {
        return deviceRepository.findByHardwareId(query.hardwareId());
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Device> handle(GetDeviceByApiKeyQuery query) {
        return deviceRepository.findByApiKey(query.apiKey());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<DeviceAssignment> handle(GetDevicesBySpaceQuery query) {
        int page = query.page() != null ? query.page() : 0;
        int size = query.size() != null ? query.size() : 20;
        return deviceAssignmentRepository.findBySpaceId(query.spaceId(), PageRequest.of(page, size));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Device> handle(GetProvisionedDevicesQuery query) {
        int cappedLimit = Math.max(1, Math.min(query.limit(), 5000));
        return deviceRepository.findAll(PageRequest.of(0, cappedLimit)).getContent();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<UUID> findSpaceIdByDeviceId(UUID deviceId) {
        return deviceAssignmentRepository.findByDeviceId(deviceId)
                .map(DeviceAssignment::getSpaceId);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isDeviceOwnedByUser(UUID deviceId, UUID userId) {
        return deviceAssignmentRepository.findByDeviceId(deviceId)
                .map(assignment -> assignment.getOwnerUserId() != null &&
                        assignment.getOwnerUserId().userId().equals(userId))
                .orElse(false);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isSpaceOwnedByUser(UUID spaceId, UUID userId) {
        return spaceRepository.findById(spaceId)
                .map(space -> space.getOwnerUserId().userId().equals(userId))
                .orElse(false);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<UUID, String> findDeviceNamesByDeviceIds(List<UUID> deviceIds) {
        if (deviceIds == null || deviceIds.isEmpty()) return Map.of();
        return deviceRepository.findAllById(deviceIds)
                .stream()
                .collect(java.util.stream.Collectors.toUnmodifiableMap(
                        Device::getId,
                        Device::getName,
                        (a, b) -> a
                ));
    }

    @Override
    @Transactional(readOnly = true)
    public Map<UUID, String> findSpaceNamesBySpaceIds(List<UUID> spaceIds) {
        if (spaceIds == null || spaceIds.isEmpty()) return Map.of();
        return spaceRepository.findAllById(spaceIds)
                .stream()
                .collect(java.util.stream.Collectors.toUnmodifiableMap(
                        Space::getId,
                        Space::getName,
                        (a, b) -> a
                ));
    }
}
