package com.claircore.device.infrastructure.persistence.jpa.repositories;

import com.claircore.device.domain.model.entities.Device;
import com.claircore.device.domain.model.valueobjects.ApiKey;
import com.claircore.device.domain.model.valueobjects.DeviceType;
import com.claircore.device.domain.model.valueobjects.HardwareId;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:device-repository;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect"
})
class DeviceRepositoryTest {
    @Autowired DeviceRepository repository;

    @Test
    void filtersByTimestampAndUsesIdCursorForEqualTimestamps() {
        Device first = device("SN-1", "HW-0001");
        Device second = device("SN-2", "HW-0002");
        repository.save(first);
        repository.save(second);
        Date watermark = first.getAuditFields().getUpdatedAt();

        var page = repository.findProvisionedDevices(watermark, first.getId(), PageRequest.of(0, 10));

        assertTrue(page.getContent().stream().noneMatch(item -> item.getDeviceId().equals(first.getId())));
    }

    @Test
    void persistsDeletedTombstoneAndReturnsItInRoster() {
        Device device = device("SN-3", "HW-0003");
        device.markDeleted();
        repository.saveAndFlush(device);

        var page = repository.findProvisionedDevices(null, null, PageRequest.of(0, 10));

        assertTrue(page.getContent().stream().anyMatch(item -> item.getDeviceId().equals(device.getId()) && item.isDeleted()));
    }

    private Device device(String serial, String hardware) {
        Device device = new Device(serial, "Sensor", new HardwareId(hardware), ApiKey.generate(), new DeviceType("air-quality-v1"));
        Date now = new Date();
        ReflectionTestUtils.setField(device.getAuditFields(), "createdAt", now);
        ReflectionTestUtils.setField(device.getAuditFields(), "updatedAt", now);
        return device;
    }
}
