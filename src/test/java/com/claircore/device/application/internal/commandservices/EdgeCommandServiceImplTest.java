package com.claircore.device.application.internal.commandservices;

import com.claircore.device.application.commandservices.EdgeCommandService;
import com.claircore.device.domain.model.aggregates.Device;
import com.claircore.device.domain.model.aggregates.DeviceAssignment;
import com.claircore.device.domain.model.aggregates.DeviceCommand;
import com.claircore.device.domain.model.commands.AcknowledgeEdgeCommandCommand;
import com.claircore.device.domain.model.queries.ClaimPendingEdgeCommandsQuery;
import com.claircore.device.domain.model.valueobjects.ApiKey;
import com.claircore.device.domain.model.valueobjects.ClaimToken;
import com.claircore.device.domain.model.valueobjects.DeviceCommandStatus;
import com.claircore.device.domain.model.valueobjects.DeviceCommandType;
import com.claircore.device.domain.model.valueobjects.DeviceStatus;
import com.claircore.device.domain.model.valueobjects.DeviceType;
import com.claircore.device.domain.model.valueobjects.EdgeCommandResult;
import com.claircore.device.domain.model.valueobjects.HardwareId;
import com.claircore.device.domain.repositories.DeviceAssignmentRepository;
import com.claircore.device.domain.repositories.DeviceCommandRepository;
import com.claircore.device.domain.repositories.DeviceRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EdgeCommandServiceImplTest {

    private static final String HARDWARE_ID = "HW-0001";

    @Mock
    private DeviceCommandRepository deviceCommandRepository;

    @Mock
    private DeviceAssignmentRepository deviceAssignmentRepository;

    @Mock
    private DeviceRepository deviceRepository;

    private EdgeCommandServiceImpl service;

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        service = new EdgeCommandServiceImpl(
                deviceCommandRepository, deviceAssignmentRepository, deviceRepository);
    }

    @Test
    void returnsOnlyTheCommandsThisCallerActuallyWon() {
        var device = device();
        var won = new DeviceCommand(device.getId(), DeviceCommandType.WAKE, "{}");
        var lost = new DeviceCommand(device.getId(), DeviceCommandType.STANDBY, "{}");
        when(deviceCommandRepository.findPendingForEdge(isNull(), any(), eq(200)))
                .thenReturn(List.of(won, lost));
        when(deviceCommandRepository.claimForEdge(eq(won.getId()), any(), any())).thenReturn(1);
        when(deviceCommandRepository.claimForEdge(eq(lost.getId()), any(), any())).thenReturn(0);
        when(deviceRepository.findAllById(List.of(device.getId()))).thenReturn(List.of(device));

        var claimed = service.handle(new ClaimPendingEdgeCommandsQuery(null, null, 200));

        assertThat(claimed).singleElement()
                .extracting(EdgeCommandService.PendingEdgeCommand::command)
                .extracting(DeviceCommand::getId).isEqualTo(won.getId());
        assertThat(claimed.getFirst().hardwareId()).isEqualTo(HARDWARE_ID);
        assertThat(won.getStatus()).isEqualTo(DeviceCommandStatus.SENT);
    }

    @Test
    void resolvesHardwareIdsInOneCallForTheWholePage() {
        var first = device();
        var second = Device.reconstitute(UUID.randomUUID(), "SN-2", "Sensor", "Sensor", false,
                new HardwareId("HW-0002"), ApiKey.generate(), new DeviceType("air-quality-v1"), null, null);
        var a = new DeviceCommand(first.getId(), DeviceCommandType.WAKE, "{}");
        var b = new DeviceCommand(second.getId(), DeviceCommandType.WAKE, "{}");
        when(deviceCommandRepository.findPendingForEdge(isNull(), any(), anyInt())).thenReturn(List.of(a, b));
        when(deviceCommandRepository.claimForEdge(any(), any(), any())).thenReturn(1);
        when(deviceRepository.findAllById(any())).thenReturn(List.of(first, second));

        var claimed = service.handle(new ClaimPendingEdgeCommandsQuery(null, null, 200));

        assertThat(claimed).extracting(EdgeCommandService.PendingEdgeCommand::hardwareId)
                .containsExactly("HW-0001", "HW-0002");
        verify(deviceRepository).findAllById(any());
    }

    @Test
    void anExpiredLeaseIsRedeliveredRatherThanReSent() {
        var device = device();
        var expired = new DeviceCommand(device.getId(), DeviceCommandType.WAKE, "{}");
        expired.markSent();
        Instant firstSentAt = expired.getSentAt();
        when(deviceCommandRepository.findPendingForEdge(isNull(), any(), anyInt())).thenReturn(List.of(expired));
        when(deviceCommandRepository.claimForEdge(any(), any(), any())).thenReturn(1);
        when(deviceRepository.findAllById(any())).thenReturn(List.of(device));

        service.handle(new ClaimPendingEdgeCommandsQuery(null, null, 200));

        assertThat(expired.getStatus()).isEqualTo(DeviceCommandStatus.SENT);
        assertThat(expired.getSentAt()).isAfterOrEqualTo(firstSentAt);
    }

    @Test
    void narrowsToOneUnitWhenAHardwareIdIsGiven() {
        var device = device();
        when(deviceCommandRepository.findPendingForEdgeByHardware(eq(HARDWARE_ID), isNull(), any(), eq(50)))
                .thenReturn(List.of());

        service.handle(new ClaimPendingEdgeCommandsQuery(HARDWARE_ID, null, 50));

        verify(deviceCommandRepository, never()).findPendingForEdge(any(), any(), anyInt());
    }

    @Test
    void anAcknowledgementFromAnotherUnitIsNotFoundRatherThanRejected() {
        var device = device();
        var command = new DeviceCommand(device.getId(), DeviceCommandType.WAKE, "{}");
        command.markSent();
        when(deviceCommandRepository.findByIdForAcknowledgement(command.getId())).thenReturn(Optional.of(command));
        when(deviceRepository.findById(device.getId())).thenReturn(Optional.of(device));

        var outcome = service.handle(new AcknowledgeEdgeCommandCommand(
                command.getId(), "HW-9999", EdgeCommandResult.EXECUTED, null));

        // Not CONFLICT and not FORBIDDEN: the caller must not learn the command exists.
        assertThat(outcome).isEqualTo(EdgeCommandService.AcknowledgementOutcome.NOT_FOUND);
        assertThat(command.getStatus()).isEqualTo(DeviceCommandStatus.SENT);
    }

    @Test
    void anAlreadyTerminalCommandConflicts() {
        var device = device();
        var command = new DeviceCommand(device.getId(), DeviceCommandType.WAKE, "{}");
        command.markSent();
        command.markExecuted();
        when(deviceCommandRepository.findByIdForAcknowledgement(command.getId())).thenReturn(Optional.of(command));
        when(deviceRepository.findById(device.getId())).thenReturn(Optional.of(device));

        var outcome = service.handle(new AcknowledgeEdgeCommandCommand(
                command.getId(), HARDWARE_ID, EdgeCommandResult.EXECUTED, null));

        assertThat(outcome).isEqualTo(EdgeCommandService.AcknowledgementOutcome.CONFLICT);
    }

    @Test
    void aPendingCommandCannotBeAcknowledgedBecauseItWasNeverDelivered() {
        var device = device();
        var command = new DeviceCommand(device.getId(), DeviceCommandType.WAKE, "{}");
        when(deviceCommandRepository.findByIdForAcknowledgement(command.getId())).thenReturn(Optional.of(command));
        when(deviceRepository.findById(device.getId())).thenReturn(Optional.of(device));

        var outcome = service.handle(new AcknowledgeEdgeCommandCommand(
                command.getId(), HARDWARE_ID, EdgeCommandResult.EXECUTED, null));

        assertThat(outcome).isEqualTo(EdgeCommandService.AcknowledgementOutcome.NOT_FOUND);
    }

    @Test
    void anUnknownCommandIsNotFound() {
        UUID commandId = UUID.randomUUID();
        when(deviceCommandRepository.findByIdForAcknowledgement(commandId)).thenReturn(Optional.empty());

        assertThat(service.handle(new AcknowledgeEdgeCommandCommand(
                commandId, HARDWARE_ID, EdgeCommandResult.EXECUTED, null)))
                .isEqualTo(EdgeCommandService.AcknowledgementOutcome.NOT_FOUND);
    }

    @Test
    void executingAStandbyCommandPutsTheAssignmentIntoStandby() {
        var device = device();
        var command = new DeviceCommand(device.getId(), DeviceCommandType.STANDBY, "{}");
        command.markSent();
        var assignment = new DeviceAssignment(device.getId(), ClaimToken.generate());
        when(deviceCommandRepository.findByIdForAcknowledgement(command.getId())).thenReturn(Optional.of(command));
        when(deviceRepository.findById(device.getId())).thenReturn(Optional.of(device));
        when(deviceAssignmentRepository.findByDeviceIdForUpdate(device.getId())).thenReturn(Optional.of(assignment));

        var outcome = service.handle(new AcknowledgeEdgeCommandCommand(
                command.getId(), HARDWARE_ID, EdgeCommandResult.EXECUTED, null));

        assertThat(outcome).isEqualTo(EdgeCommandService.AcknowledgementOutcome.OK);
        assertThat(assignment.getStatus()).isEqualTo(DeviceStatus.STANDBY);
        verify(deviceAssignmentRepository).save(assignment);
    }

    @Test
    void aFailedAcknowledgementRecordsTheReasonAndLeavesTheAssignmentAlone() {
        var device = device();
        var command = new DeviceCommand(device.getId(), DeviceCommandType.WAKE, "{}");
        command.markSent();
        when(deviceCommandRepository.findByIdForAcknowledgement(command.getId())).thenReturn(Optional.of(command));
        when(deviceRepository.findById(device.getId())).thenReturn(Optional.of(device));

        var outcome = service.handle(new AcknowledgeEdgeCommandCommand(
                command.getId(), HARDWARE_ID, EdgeCommandResult.FAILED, "radio down"));

        assertThat(outcome).isEqualTo(EdgeCommandService.AcknowledgementOutcome.OK);
        assertThat(command.getStatus()).isEqualTo(DeviceCommandStatus.FAILED);
        assertThat(command.getFailureReason()).isEqualTo("radio down");
        verify(deviceAssignmentRepository, never()).save(any());
    }

    @Test
    void anAcknowledgementForAPreviousAssignmentGenerationIsVoidedAndConflicts() {
        var device = device();
        var previousGeneration = UUID.randomUUID();
        var command = new DeviceCommand(device.getId(), previousGeneration, DeviceCommandType.STANDBY, "{}");
        command.markSent();
        var current = new DeviceAssignment(device.getId(), ClaimToken.generate());
        when(deviceCommandRepository.findByIdForAcknowledgement(command.getId())).thenReturn(Optional.of(command));
        when(deviceRepository.findById(device.getId())).thenReturn(Optional.of(device));
        when(deviceAssignmentRepository.findByDeviceIdForUpdate(device.getId())).thenReturn(Optional.of(current));
        var outcome = service.handle(new AcknowledgeEdgeCommandCommand(
                command.getId(), HARDWARE_ID, EdgeCommandResult.EXECUTED, null));
        assertThat(outcome).isEqualTo(EdgeCommandService.AcknowledgementOutcome.CONFLICT);
        assertThat(command.getStatus()).isEqualTo(DeviceCommandStatus.EXPIRED);
        // The new owner's assignment must not move to STANDBY because of the old owner's command.
        assertThat(current.getStatus()).isEqualTo(DeviceStatus.OFFLINE);
        verify(deviceAssignmentRepository, never()).save(any());
        verify(deviceCommandRepository).save(command);
    }

    @Test
    void anAcknowledgementAfterTheAssignmentWasUnlinkedIsVoidedAndConflicts() {
        var device = device();
        var command = new DeviceCommand(device.getId(), UUID.randomUUID(), DeviceCommandType.WAKE, "{}");
        command.markSent();
        when(deviceCommandRepository.findByIdForAcknowledgement(command.getId())).thenReturn(Optional.of(command));
        when(deviceRepository.findById(device.getId())).thenReturn(Optional.of(device));
        when(deviceAssignmentRepository.findByDeviceIdForUpdate(device.getId())).thenReturn(Optional.empty());
        var outcome = service.handle(new AcknowledgeEdgeCommandCommand(
                command.getId(), HARDWARE_ID, EdgeCommandResult.EXECUTED, null));
        assertThat(outcome).isEqualTo(EdgeCommandService.AcknowledgementOutcome.CONFLICT);
        assertThat(command.getStatus()).isEqualTo(DeviceCommandStatus.EXPIRED);
    }

    private static Device device() {
        return Device.reconstitute(UUID.randomUUID(), "SN-1", "Sensor", "Sensor", false,
                new HardwareId(HARDWARE_ID), ApiKey.generate(), new DeviceType("air-quality-v1"), null, null);
    }
}
