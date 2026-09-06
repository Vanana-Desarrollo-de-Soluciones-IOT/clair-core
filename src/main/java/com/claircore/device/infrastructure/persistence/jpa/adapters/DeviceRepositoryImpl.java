package com.claircore.device.infrastructure.persistence.jpa.adapters;

import com.claircore.device.domain.model.aggregates.Device;
import com.claircore.device.domain.model.valueobjects.ApiKey;
import com.claircore.device.domain.model.valueobjects.HardwareId;
import com.claircore.device.domain.model.valueobjects.ProvisionedDevice;
import com.claircore.device.domain.repositories.DeviceRepository;
import com.claircore.device.infrastructure.persistence.jpa.assemblers.DevicePersistenceAssembler;
import com.claircore.device.infrastructure.persistence.jpa.repositories.DevicePersistenceRepository;
import com.claircore.shared.domain.model.PageResult;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class DeviceRepositoryImpl implements DeviceRepository {

    /** Lower than any stored timestamp or id, so the first page is unfiltered. See the query. */
    private static final Instant CURSOR_START = Instant.EPOCH;
    private static final UUID ID_CURSOR_START = new UUID(0L, 0L);

    private final DevicePersistenceRepository devicePersistenceRepository;

    public DeviceRepositoryImpl(DevicePersistenceRepository devicePersistenceRepository) {
        this.devicePersistenceRepository = devicePersistenceRepository;
    }

    @Override
    public Device save(Device device) {
        var saved = devicePersistenceRepository.save(DevicePersistenceAssembler.toPersistenceFromDomain(device));
        return DevicePersistenceAssembler.toDomainFromPersistence(saved);
    }

    @Override
    public List<Device> saveAll(Collection<Device> devices) {
        var entities = devices.stream().map(DevicePersistenceAssembler::toPersistenceFromDomain).toList();
        return devicePersistenceRepository.saveAll(entities).stream()
                .map(DevicePersistenceAssembler::toDomainFromPersistence)
                .toList();
    }

    @Override
    public Optional<Device> findById(UUID id) {
        return devicePersistenceRepository.findById(id).map(DevicePersistenceAssembler::toDomainFromPersistence);
    }

    @Override
    public List<Device> findAllById(Collection<UUID> ids) {
        if (ids == null || ids.isEmpty()) return List.of();
        return devicePersistenceRepository.findAllById(ids).stream()
                .map(DevicePersistenceAssembler::toDomainFromPersistence)
                .toList();
    }

    @Override
    public Optional<Device> findBySerialNumber(String serialNumber) {
        return devicePersistenceRepository.findBySerialNumber(serialNumber)
                .map(DevicePersistenceAssembler::toDomainFromPersistence);
    }

    @Override
    public List<Device> findAllBySerialNumberIn(Collection<String> serialNumbers) {
        if (serialNumbers == null || serialNumbers.isEmpty()) return List.of();
        return devicePersistenceRepository.findAllBySerialNumberIn(serialNumbers).stream()
                .map(DevicePersistenceAssembler::toDomainFromPersistence)
                .toList();
    }

    @Override
    public Optional<Device> findByHardwareId(String hardwareId) {
        return devicePersistenceRepository.findByHardwareId(new HardwareId(hardwareId))
                .map(DevicePersistenceAssembler::toDomainFromPersistence);
    }

    @Override
    public Optional<Device> findByApiKey(String apiKey) {
        return devicePersistenceRepository.findByApiKey(new ApiKey(apiKey))
                .map(DevicePersistenceAssembler::toDomainFromPersistence);
    }

    @Override
    public boolean existsByHardwareId(String hardwareId) {
        return devicePersistenceRepository.existsByHardwareId(new HardwareId(hardwareId));
    }

    @Override
    public PageResult<ProvisionedDevice> findProvisionedDevices(Instant since, UUID afterId, int limit) {
        var page = devicePersistenceRepository.findProvisionedDevicesForCursor(
                since != null ? since : CURSOR_START,
                afterId != null ? afterId : ID_CURSOR_START,
                PageRequest.of(0, limit));
        var rows = page.getContent().stream()
                .map(row -> new ProvisionedDevice(
                        row.getDeviceId(), row.getHardwareId(), row.getApiKey(),
                        row.getStatus(), row.isDeleted(), row.getUpdatedAt()))
                .toList();
        return new PageResult<>(rows, 0, limit, page.getTotalElements());
    }
}
