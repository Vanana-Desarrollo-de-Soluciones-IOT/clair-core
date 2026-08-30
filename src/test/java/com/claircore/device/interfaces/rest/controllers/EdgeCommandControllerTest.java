package com.claircore.device.interfaces.rest.controllers;

import com.claircore.device.domain.model.entities.DeviceCommand;
import com.claircore.device.domain.model.valueobjects.DeviceCommandStatus;
import com.claircore.device.infrastructure.persistence.jpa.repositories.DeviceCommandRepository;
import com.claircore.device.infrastructure.persistence.jpa.repositories.DeviceAssignmentRepository;
import com.claircore.device.application.internal.commandservices.EdgeCommandAcknowledgementService;
import com.claircore.device.interfaces.rest.resources.EdgeCommandAckRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class EdgeCommandControllerTest {
    @Test void pendingEndpointClaimsPendingCommandAsSent() {
        var repository = mock(DeviceCommandRepository.class);
        var assignments = mock(DeviceAssignmentRepository.class);
        var command = mock(DeviceCommand.class);
        var device = mock(com.claircore.device.domain.model.entities.Device.class);
        when(command.getStatus()).thenReturn(DeviceCommandStatus.PENDING);
        when(command.getDevice()).thenReturn(device);
        when(command.getId()).thenReturn(UUID.randomUUID());
        when(command.getPayload()).thenReturn("{}");
        when(command.getType()).thenReturn(com.claircore.device.domain.model.valueobjects.DeviceCommandType.WAKE);
        when(device.getId()).thenReturn(UUID.randomUUID());
        when(device.getHardwareId()).thenReturn(new com.claircore.device.domain.model.valueobjects.HardwareId("HW-0001"));
        when(command.getAuditFields()).thenReturn(mock(DeviceCommand.DeviceCommandAudit.class));
        when(repository.findPendingForEdge((Instant) isNull(), any(), any())).thenReturn(java.util.List.of(command));
        when(repository.claimForEdge(eq(command.getId()), any(), any())).thenReturn(1);
        var controller = new EdgeCommandController(new EdgeCommandAcknowledgementService(repository, assignments), new ObjectMapper());

        assertEquals(1, controller.pending(null, null, 10).size());
        verify(command).markSent();
        verify(repository).claimForEdge(eq(command.getId()), any(), any());
    }

    @Test void rejectsAckForWrongHardware() {
        var repository = mock(DeviceCommandRepository.class); var command = mock(DeviceCommand.class);
        var device = mock(com.claircore.device.domain.model.entities.Device.class);
        when(repository.findByIdForAcknowledgement(any())).thenReturn(Optional.of(command)); when(command.getStatus()).thenReturn(DeviceCommandStatus.PENDING);
        when(command.getDevice()).thenReturn(device); when(device.getHardwareId()).thenReturn(new com.claircore.device.domain.model.valueobjects.HardwareId("HW-0001"));
        var response = new EdgeCommandController(new EdgeCommandAcknowledgementService(repository, mock(DeviceAssignmentRepository.class)), new ObjectMapper()).acknowledge(UUID.randomUUID(), new EdgeCommandAckRequest("HW-0002", EdgeCommandAckRequest.Result.OK, null));
        assertEquals(404, response.getStatusCode().value()); verify(command, never()).markExecuted();
    }
    @Test void concurrentSecondAcknowledgementConflictsAfterFirstTerminalTransition() {
        var repository = mock(DeviceCommandRepository.class); var command = mock(DeviceCommand.class);
        var device = mock(com.claircore.device.domain.model.entities.Device.class);
        when(repository.findByIdForAcknowledgement(any())).thenReturn(Optional.of(command));
        when(command.getStatus()).thenReturn(DeviceCommandStatus.SENT, DeviceCommandStatus.EXECUTED);
        when(command.getDevice()).thenReturn(device);
        when(device.getHardwareId()).thenReturn(new com.claircore.device.domain.model.valueobjects.HardwareId("HW-0001"));
        var controller = new EdgeCommandController(new EdgeCommandAcknowledgementService(repository, mock(DeviceAssignmentRepository.class)), new ObjectMapper());
        var body = new EdgeCommandAckRequest("HW-0001", EdgeCommandAckRequest.Result.OK, null);
        assertEquals(200, controller.acknowledge(UUID.randomUUID(), body).getStatusCode().value());
        assertEquals(409, controller.acknowledge(UUID.randomUUID(), body).getStatusCode().value());
        verify(repository, times(1)).save(command);
    }

    @Test void returnsConflictForIdempotentAck() {
        var repository = mock(DeviceCommandRepository.class); var command = mock(DeviceCommand.class);
        var device = mock(com.claircore.device.domain.model.entities.Device.class);
        when(repository.findByIdForAcknowledgement(any())).thenReturn(Optional.of(command)); when(command.getStatus()).thenReturn(DeviceCommandStatus.EXECUTED);
        when(command.getDevice()).thenReturn(device);
        when(device.getHardwareId()).thenReturn(new com.claircore.device.domain.model.valueobjects.HardwareId("HW-0001"));
        var response = new EdgeCommandController(new EdgeCommandAcknowledgementService(repository, mock(DeviceAssignmentRepository.class)), new ObjectMapper()).acknowledge(UUID.randomUUID(), new EdgeCommandAckRequest("HW-0001", EdgeCommandAckRequest.Result.OK, null));
        assertEquals(409, response.getStatusCode().value());
    }

    @Test void doesNotRevealTerminalCommandToWrongHardware() {
        var repository = mock(DeviceCommandRepository.class); var command = mock(DeviceCommand.class);
        var device = mock(com.claircore.device.domain.model.entities.Device.class);
        when(repository.findByIdForAcknowledgement(any())).thenReturn(Optional.of(command));
        when(command.getStatus()).thenReturn(DeviceCommandStatus.EXECUTED);
        when(command.getDevice()).thenReturn(device);
        when(device.getHardwareId()).thenReturn(new com.claircore.device.domain.model.valueobjects.HardwareId("HW-0001"));
        var response = new EdgeCommandController(new EdgeCommandAcknowledgementService(repository, mock(DeviceAssignmentRepository.class)), new ObjectMapper()).acknowledge(UUID.randomUUID(), new EdgeCommandAckRequest("HW-0002", EdgeCommandAckRequest.Result.OK, null));
        assertEquals(404, response.getStatusCode().value());
    }
}
