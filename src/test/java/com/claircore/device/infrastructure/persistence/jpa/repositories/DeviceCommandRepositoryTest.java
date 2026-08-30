package com.claircore.device.infrastructure.persistence.jpa.repositories;

import com.claircore.device.domain.model.entities.Device;
import com.claircore.device.domain.model.entities.DeviceCommand;
import com.claircore.device.domain.model.valueobjects.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.Lock;
import jakarta.persistence.LockModeType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.Date;
import java.util.List;
import static org.junit.jupiter.api.Assertions.assertEquals;

@DataJpaTest
@TestPropertySource(properties = {
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect"
})
class DeviceCommandRepositoryTest {
    @Autowired DeviceRepository devices;
    @Autowired DeviceCommandRepository commands;

    @Test
    void globalPendingQueryUsesPessimisticLock() throws NoSuchMethodException {
        Lock lock = DeviceCommandRepository.class.getMethod(
                "findPendingForEdge", java.time.Instant.class, java.time.Instant.class,
                org.springframework.data.domain.Pageable.class)
                .getAnnotation(Lock.class);
        assertEquals(LockModeType.PESSIMISTIC_WRITE, lock.value());
    }

    @Test
    void hardwareScopedPendingQueryUsesPessimisticLock() throws NoSuchMethodException {
        Lock lock = DeviceCommandRepository.class.getMethod(
                "findPendingForEdgeByHardware", String.class, java.time.Instant.class,
                java.time.Instant.class, org.springframework.data.domain.Pageable.class)
                .getAnnotation(Lock.class);
        assertEquals(LockModeType.PESSIMISTIC_WRITE, lock.value());
    }

    @Test
    void expiredSentIsRedeliveredBeforeSinceWithoutDroppingNewPending() {
        Device device = saveDevice("SN-1003", "HW-1003");
        DeviceCommand expired = new DeviceCommand(device, DeviceCommandType.WAKE, "{}");
        expired.markSent();
        ReflectionTestUtils.setField(expired, "sentAt", java.time.Instant.parse("2024-01-01T00:00:00Z"));
        ReflectionTestUtils.setField(expired.getAuditFields(), "createdAt", new Date(0L));
        ReflectionTestUtils.setField(expired.getAuditFields(), "updatedAt", new Date(0L));
        commands.saveAndFlush(expired);

        DeviceCommand pending = new DeviceCommand(device, DeviceCommandType.WAKE, "{}");
        Date newer = Date.from(java.time.Instant.parse("2025-01-01T00:00:00Z"));
        ReflectionTestUtils.setField(pending.getAuditFields(), "createdAt", newer);
        ReflectionTestUtils.setField(pending.getAuditFields(), "updatedAt", newer);
        commands.saveAndFlush(pending);

        List<DeviceCommand> result = commands.findPendingForEdge(
                java.time.Instant.parse("2025-01-01T00:00:00Z"),
                java.time.Instant.parse("2024-06-01T00:00:00Z"), PageRequest.of(0, 10));
        assertEquals(2, result.size());
        assertEquals(expired.getId(), result.get(0).getId());
        assertEquals(pending.getId(), result.get(1).getId());
    }

    @Test
    void appliesHardwareFilterBeforeLimit() {
        Device other = saveDevice("SN-1001", "HW-1001");
        Device wanted = saveDevice("SN-1002", "HW-1002");
        DeviceCommand otherCommand = new DeviceCommand(other, DeviceCommandType.WAKE, "{}");
        DeviceCommand wantedCommand = new DeviceCommand(wanted, DeviceCommandType.WAKE, "{}");
        Date now = new Date();
        ReflectionTestUtils.setField(otherCommand.getAuditFields(), "createdAt", now);
        ReflectionTestUtils.setField(otherCommand.getAuditFields(), "updatedAt", now);
        ReflectionTestUtils.setField(wantedCommand.getAuditFields(), "createdAt", now);
        ReflectionTestUtils.setField(wantedCommand.getAuditFields(), "updatedAt", now);
        commands.save(otherCommand); commands.save(wantedCommand);

        List<DeviceCommand> result = commands.findPendingForEdgeByHardware(
                "HW-1002", null, java.time.Instant.now().plusSeconds(1), PageRequest.of(0, 1));
        assertEquals(1, result.size());
        assertEquals(wanted.getId(), result.getFirst().getDevice().getId());
    }

    private Device saveDevice(String serial, String hardware) {
        Device device = new Device(serial, "Sensor", new HardwareId(hardware), ApiKey.generate(), new DeviceType("air-quality-v1"));
        Date now = new Date();
        ReflectionTestUtils.setField(device.getAuditFields(), "createdAt", now);
        ReflectionTestUtils.setField(device.getAuditFields(), "updatedAt", now);
        return devices.saveAndFlush(device);
    }
}
