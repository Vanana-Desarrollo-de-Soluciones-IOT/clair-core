package com.claircore.device.domain.model.aggregates;

import com.claircore.device.domain.model.valueobjects.ApiKey;
import com.claircore.device.domain.model.valueobjects.DeviceCommandStatus;
import com.claircore.device.domain.model.valueobjects.DeviceCommandType;
import com.claircore.device.domain.model.valueobjects.DeviceType;
import com.claircore.device.domain.model.valueobjects.HardwareId;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class DeviceCommandTest {

    @Test
    void shouldStartPendingWhenCommandIsCreated() {
        DeviceCommand command = new DeviceCommand(device().getId(), DeviceCommandType.WAKE, "{}");

        assertEquals(DeviceCommandStatus.PENDING, command.getStatus());
    }

    @Test
    void shouldMarkExecutedWhenCommandIsExecuted() {
        DeviceCommand command = new DeviceCommand(device().getId(), DeviceCommandType.RESTART, "{}");

        command.markExecuted();

        assertEquals(DeviceCommandStatus.EXECUTED, command.getStatus());
        assertNotNull(command.getExecutedAt());
        assertEquals(null, command.getFailureReason());
    }

    @Test
    void shouldMarkFailedWhenCommandFails() {
        DeviceCommand command = new DeviceCommand(device().getId(), DeviceCommandType.STANDBY, "{}");

        command.markFailed("timeout");

        assertEquals(DeviceCommandStatus.FAILED, command.getStatus());
        assertEquals("timeout", command.getFailureReason());
        assertNotNull(command.getExecutedAt());
    }

    private Device device() {
        return new Device(
                "SN-0003",
                "Sensor 0003",
                new HardwareId("CLAIR-0KBG"),
                ApiKey.generate(),
                new DeviceType("air-quality-v1")
        );
    }
}
