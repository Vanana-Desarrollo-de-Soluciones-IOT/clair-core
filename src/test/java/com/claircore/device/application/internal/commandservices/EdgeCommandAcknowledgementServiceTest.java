package com.claircore.device.application.internal.commandservices;

import com.claircore.device.domain.model.entities.Device;
import com.claircore.device.domain.model.entities.DeviceAssignment;
import com.claircore.device.domain.model.entities.DeviceCommand;
import com.claircore.device.domain.model.valueobjects.DeviceCommandStatus;
import com.claircore.device.domain.model.valueobjects.DeviceCommandType;
import com.claircore.device.domain.model.valueobjects.HardwareId;
import com.claircore.device.infrastructure.persistence.jpa.repositories.DeviceAssignmentRepository;
import com.claircore.device.infrastructure.persistence.jpa.repositories.DeviceCommandRepository;
import com.claircore.device.interfaces.rest.resources.EdgeCommandAckRequest;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class EdgeCommandAcknowledgementServiceTest {
    @Test
    void transactionFlowClaimsPendingThenAcknowledgesSentCommand() {
        var repository = mock(DeviceCommandRepository.class);
        var command = mock(DeviceCommand.class);
        var commandId = UUID.randomUUID();
        var device = mock(Device.class);
        when(command.getId()).thenReturn(commandId);
        when(command.getStatus()).thenReturn(DeviceCommandStatus.PENDING, DeviceCommandStatus.SENT);
        when(command.getDevice()).thenReturn(device);
        when(device.getHardwareId()).thenReturn(new HardwareId("CLAIR-0KBG"));
        when(repository.findPendingForEdge((Instant) isNull(), any(), any())).thenReturn(java.util.List.of(command));
        when(repository.claimForEdge(eq(commandId), any(), any())).thenReturn(1);
        when(repository.findByIdForAcknowledgement(commandId)).thenReturn(Optional.of(command));
        var service = new EdgeCommandAcknowledgementService(repository, mock(DeviceAssignmentRepository.class));

        assertEquals(1, service.claimForEdge(null, null, 1).size());
        assertEquals(EdgeCommandAcknowledgementService.Outcome.OK, service.acknowledge(commandId,
                new EdgeCommandAckRequest("CLAIR-0KBG", EdgeCommandAckRequest.Result.OK, null)));
        verify(command).markSent();
        verify(command).markExecuted();
    }

    @Test
    void claimsPendingCommandAsSentBeforeEdgeDelivery() {
        var repository = mock(DeviceCommandRepository.class);
        var command = mock(DeviceCommand.class);
        var commandId = UUID.randomUUID();
        when(command.getId()).thenReturn(commandId);
        when(repository.findById(commandId)).thenReturn(Optional.of(command));
        when(repository.claimForEdge(eq(commandId), any(), any())).thenReturn(1);
        var result = new EdgeCommandAcknowledgementService(repository, mock(DeviceAssignmentRepository.class))
                .claimForEdge(java.util.List.of(command));
        assertEquals(java.util.List.of(command), result);
        verify(repository, never()).save(command);
    }

    @Test
    void executesSentCommandAndUpdatesHardwareAssignment() {
        var commandId = UUID.randomUUID();
        var deviceId = UUID.randomUUID();
        var repository = mock(DeviceCommandRepository.class);
        var assignments = mock(DeviceAssignmentRepository.class);
        var command = mock(DeviceCommand.class);
        var device = mock(Device.class);
        var assignment = mock(DeviceAssignment.class);
        when(repository.findByIdForAcknowledgement(commandId)).thenReturn(Optional.of(command));
        when(command.getDevice()).thenReturn(device);
        when(device.getId()).thenReturn(deviceId);
        when(device.getHardwareId()).thenReturn(new HardwareId("CLAIR-0KBG"));
        when(command.getStatus()).thenReturn(DeviceCommandStatus.SENT);
        when(command.getType()).thenReturn(DeviceCommandType.WAKE);
        when(assignments.findByDeviceId(deviceId)).thenReturn(Optional.of(assignment));

        var result = new EdgeCommandAcknowledgementService(repository, assignments).acknowledge(
                commandId,
                new EdgeCommandAckRequest("CLAIR-0KBG", EdgeCommandAckRequest.Result.OK, null));

        assertEquals(EdgeCommandAcknowledgementService.Outcome.OK, result);
        verify(command).markExecuted();
        verify(repository).save(command);
        verify(assignment).markOnline();
        verify(assignments).save(assignment);
    }

    @Test
    void terminalCommandIsConflictAndHasNoSideEffects() {
        var repository = mock(DeviceCommandRepository.class);
        var assignments = mock(DeviceAssignmentRepository.class);
        var command = mock(DeviceCommand.class);
        var device = mock(Device.class);
        when(repository.findByIdForAcknowledgement(any())).thenReturn(Optional.of(command));
        when(command.getDevice()).thenReturn(device);
        when(device.getHardwareId()).thenReturn(new HardwareId("CLAIR-0KBG"));
        when(command.getStatus()).thenReturn(DeviceCommandStatus.EXECUTED);

        var result = new EdgeCommandAcknowledgementService(repository, assignments).acknowledge(
                UUID.randomUUID(),
                new EdgeCommandAckRequest("CLAIR-0KBG", EdgeCommandAckRequest.Result.OK, null));

        assertEquals(EdgeCommandAcknowledgementService.Outcome.CONFLICT, result);
        verify(command, never()).markExecuted();
        verify(repository, never()).save(any());
        verifyNoInteractions(assignments);
    }

    @Test
    void wrongHardwareIsNotFoundBeforeTerminalityIsInspected() {
        var repository = mock(DeviceCommandRepository.class);
        var assignments = mock(DeviceAssignmentRepository.class);
        var command = mock(DeviceCommand.class);
        var device = mock(Device.class);
        when(repository.findByIdForAcknowledgement(any())).thenReturn(Optional.of(command));
        when(command.getDevice()).thenReturn(device);
        when(device.getHardwareId()).thenReturn(new HardwareId("CLAIR-0KBG"));

        var result = new EdgeCommandAcknowledgementService(repository, assignments).acknowledge(
                UUID.randomUUID(),
                new EdgeCommandAckRequest("HW-2", EdgeCommandAckRequest.Result.OK, null));

        assertEquals(EdgeCommandAcknowledgementService.Outcome.NOT_FOUND, result);
        verify(command, never()).getStatus();
        verifyNoInteractions(assignments);
    }
}
