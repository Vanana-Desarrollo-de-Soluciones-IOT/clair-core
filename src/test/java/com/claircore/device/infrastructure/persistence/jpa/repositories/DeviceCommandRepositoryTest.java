package com.claircore.device.infrastructure.persistence.jpa.repositories;

import com.claircore.device.domain.model.entities.Device;
import com.claircore.device.domain.model.entities.DeviceCommand;
import com.claircore.device.domain.model.valueobjects.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.Date;
import java.util.List;
import static org.junit.jupiter.api.Assertions.assertEquals;

@DataJpaTest
@TestPropertySource(properties = "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect")
class DeviceCommandRepositoryTest {
    @Autowired DeviceRepository devices;
    @Autowired DeviceCommandRepository commands;

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

        List<DeviceCommand> result = commands.findPendingForEdgeByHardware(List.of(DeviceCommandStatus.PENDING), "HW-1002", null, PageRequest.of(0, 1));
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
