package com.claircore.device.interfaces.rest.controllers;

import com.claircore.device.domain.model.entities.DeviceCommand;
import com.claircore.device.domain.model.valueobjects.DeviceCommandStatus;
import com.claircore.device.infrastructure.persistence.jpa.repositories.DeviceCommandRepository;
import com.claircore.device.interfaces.rest.resources.EdgeCommandAckRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class EdgeCommandControllerTest {
    @Test void rejectsAckForWrongHardware() {
        var repository = mock(DeviceCommandRepository.class); var command = mock(DeviceCommand.class);
        var device = mock(com.claircore.device.domain.model.entities.Device.class);
        when(repository.findById(any())).thenReturn(Optional.of(command)); when(command.getStatus()).thenReturn(DeviceCommandStatus.PENDING);
        when(command.getDevice()).thenReturn(device); when(device.getHardwareId()).thenReturn(new com.claircore.device.domain.model.valueobjects.HardwareId("HW-0001"));
        var response = new EdgeCommandController(repository, new ObjectMapper()).acknowledge(UUID.randomUUID(), new EdgeCommandAckRequest("HW-0002", Instant.now(), EdgeCommandAckRequest.Result.OK, null));
        assertEquals(404, response.getStatusCode().value()); verify(command, never()).markExecuted();
    }
    @Test void returnsConflictForIdempotentAck() {
        var repository = mock(DeviceCommandRepository.class); var command = mock(DeviceCommand.class);
        when(repository.findById(any())).thenReturn(Optional.of(command)); when(command.getStatus()).thenReturn(DeviceCommandStatus.EXECUTED);
        var response = new EdgeCommandController(repository, new ObjectMapper()).acknowledge(UUID.randomUUID(), new EdgeCommandAckRequest("HW-0001", Instant.now(), EdgeCommandAckRequest.Result.OK, null));
        assertEquals(409, response.getStatusCode().value());
    }
}
