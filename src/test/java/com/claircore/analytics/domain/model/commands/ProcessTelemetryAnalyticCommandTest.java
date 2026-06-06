package com.claircore.analytics.domain.model.commands;

import com.claircore.analytics.domain.model.valueobjects.DeviceId;
import org.junit.jupiter.api.Test;
import java.time.Instant;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

class ProcessTelemetryAnalyticCommandTest {

    @Test
    void shouldCreateCommandWhenDataIsValid() {
        var deviceId = new DeviceId(UUID.randomUUID());
        var command = new ProcessTelemetryAnalyticCommand(
                deviceId,
                500.0,
                15.0,
                22.5,
                45.0,
                Instant.now()
        );
        assertNotNull(command);
        assertEquals(deviceId, command.deviceId());
        assertEquals(500.0, command.co2());
        assertEquals(15.0, command.pm2_5());
        assertEquals(22.5, command.temperature());
        assertEquals(45.0, command.humidity());
        assertNotNull(command.recordedAt());
    }

    @Test
    void shouldThrowExceptionWhenDeviceIdIsNull() {
        assertThrows(IllegalArgumentException.class, () ->
                new ProcessTelemetryAnalyticCommand(null, 500.0, 15.0, 22.5, 45.0, Instant.now())
        );
    }

    @Test
    void shouldThrowExceptionWhenCo2IsNull() {
        var deviceId = new DeviceId(UUID.randomUUID());
        assertThrows(IllegalArgumentException.class, () ->
                new ProcessTelemetryAnalyticCommand(deviceId, null, 15.0, 22.5, 45.0, Instant.now())
        );
    }

    @Test
    void shouldThrowExceptionWhenCo2IsNegative() {
        var deviceId = new DeviceId(UUID.randomUUID());
        assertThrows(IllegalArgumentException.class, () ->
                new ProcessTelemetryAnalyticCommand(deviceId, -1.0, 15.0, 22.5, 45.0, Instant.now())
        );
    }

    @Test
    void shouldThrowExceptionWhenPm25IsNull() {
        var deviceId = new DeviceId(UUID.randomUUID());
        assertThrows(IllegalArgumentException.class, () ->
                new ProcessTelemetryAnalyticCommand(deviceId, 500.0, null, 22.5, 45.0, Instant.now())
        );
    }

    @Test
    void shouldThrowExceptionWhenPm25IsNegative() {
        var deviceId = new DeviceId(UUID.randomUUID());
        assertThrows(IllegalArgumentException.class, () ->
                new ProcessTelemetryAnalyticCommand(deviceId, 500.0, -1.0, 22.5, 45.0, Instant.now())
        );
    }

    @Test
    void shouldThrowExceptionWhenTemperatureIsNull() {
        var deviceId = new DeviceId(UUID.randomUUID());
        assertThrows(IllegalArgumentException.class, () ->
                new ProcessTelemetryAnalyticCommand(deviceId, 500.0, 15.0, null, 45.0, Instant.now())
        );
    }

    @Test
    void shouldThrowExceptionWhenHumidityIsNull() {
        var deviceId = new DeviceId(UUID.randomUUID());
        assertThrows(IllegalArgumentException.class, () ->
                new ProcessTelemetryAnalyticCommand(deviceId, 500.0, 15.0, 22.5, null, Instant.now())
        );
    }

    @Test
    void shouldThrowExceptionWhenRecordedAtIsNull() {
        var deviceId = new DeviceId(UUID.randomUUID());
        assertThrows(IllegalArgumentException.class, () ->
                new ProcessTelemetryAnalyticCommand(deviceId, 500.0, 15.0, 22.5, 45.0, null)
        );
    }
}
