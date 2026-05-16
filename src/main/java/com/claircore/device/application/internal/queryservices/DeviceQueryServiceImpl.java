package com.claircore.device.application.internal.queryservices;

import com.claircore.device.domain.model.entities.Device;
import com.claircore.device.domain.model.entities.Space;
import com.claircore.device.domain.model.queries.*;
import com.claircore.device.domain.services.DeviceQueryService;
import com.claircore.device.infrastructure.persistence.jpa.repositories.DeviceRepository;
import com.claircore.device.infrastructure.persistence.jpa.repositories.SpaceRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class DeviceQueryServiceImpl implements DeviceQueryService {

    private final SpaceRepository spaceRepository;
    private final DeviceRepository deviceRepository;

    public DeviceQueryServiceImpl(SpaceRepository spaceRepository, DeviceRepository deviceRepository) {
        this.spaceRepository = spaceRepository;
        this.deviceRepository = deviceRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Space> handle(GetSpaceByIdQuery query) {
        return spaceRepository.findById(query.spaceId());
    }

    @Override
    @Transactional(readOnly = true)
    public List<Space> handle(GetSpacesByOwnerQuery query) {
        return spaceRepository.findByOwnerUserId(query.ownerUserId());
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
    public Page<Device> handle(GetDevicesBySpaceQuery query) {
        int page = query.page() != null ? query.page() : 0;
        int size = query.size() != null ? query.size() : 20;
        return deviceRepository.findBySpaceId(query.spaceId(), PageRequest.of(page, size));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Device> findBySpaceId(UUID spaceId) {
        return deviceRepository.findBySpaceId(spaceId);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Device> findById(UUID id) {
        return deviceRepository.findById(id);
    }
}