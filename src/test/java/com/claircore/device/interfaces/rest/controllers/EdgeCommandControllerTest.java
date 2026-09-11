package com.claircore.device.interfaces.rest.controllers;

import com.claircore.device.application.commandservices.EdgeCommandService;
import com.claircore.device.domain.model.aggregates.DeviceCommand;
import com.claircore.device.domain.model.queries.ClaimPendingEdgeCommandsQuery;
import com.claircore.device.domain.model.valueobjects.DeviceCommandType;
import com.claircore.device.domain.model.valueobjects.EdgeCommandResult;
import com.claircore.device.interfaces.rest.resources.EdgeCommandAckRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * The HTTP contract only. What claiming and acknowledging actually do to a command is
 * {@code EdgeCommandServiceImplTest}'s subject.
 */
class EdgeCommandControllerTest {
    private static final UUID ASSIGNMENT_ID = UUID.fromString("7d5b1e2c-3a4f-4b6d-8c9e-0f1a2b3c4d5e");

    private final EdgeCommandService service = mock(EdgeCommandService.class);
    private final EdgeCommandController controller = new EdgeCommandController(service, new ObjectMapper());

    @Test
    void rendersEveryFieldTheEdgeFirmwareReadsInSnakeCase() {
        UUID deviceId = UUID.randomUUID();
        var command = DeviceCommand.reconstitute(
                UUID.randomUUID(), deviceId, ASSIGNMENT_ID, DeviceCommandType.WAKE,
                com.claircore.device.domain.model.valueobjects.DeviceCommandStatus.SENT,
                "{\"level\":3}", null, null, null, Instant.parse("2026-05-16T22:30:00Z"), null);
        when(service.handle(any(ClaimPendingEdgeCommandsQuery.class)))
                .thenReturn(List.of(new EdgeCommandService.PendingEdgeCommand(command, "HW-0001")));

        var resource = controller.pending(null, null, 10).getFirst();

        assertThat(resource.commandId()).isEqualTo(command.getId().toString());
        assertThat(resource.deviceId()).isEqualTo(deviceId.toString());
        assertThat(resource.hardwareId()).isEqualTo("HW-0001");
        assertThat(resource.commandType()).isEqualTo("WAKE");
        assertThat(resource.issuedAt()).isEqualTo("2026-05-16T22:30:00Z");
        // A JSON payload stays JSON, as the hand-built map made it.
        assertThat(resource.payload()).isInstanceOf(ObjectNode.class);
        assertThat(((ObjectNode) resource.payload()).get("level").asInt()).isEqualTo(3);
    }

    @Test
    void aPayloadThatIsNotJsonIsSentAsTheStringItWasStoredAs() {
        var command = DeviceCommand.reconstitute(
                UUID.randomUUID(), UUID.randomUUID(), null, DeviceCommandType.RESTART,
                com.claircore.device.domain.model.valueobjects.DeviceCommandStatus.SENT,
                "not json", null, null, null, Instant.now(), null);
        when(service.handle(any(ClaimPendingEdgeCommandsQuery.class)))
                .thenReturn(List.of(new EdgeCommandService.PendingEdgeCommand(command, "HW-0001")));

        assertThat(controller.pending(null, null, 10).getFirst().payload()).isEqualTo("not json");
    }

    @Test
    void aCommandWithNoCreationTimestampIssuesAnEmptyStringNotNull() {
        var command = DeviceCommand.reconstitute(
                UUID.randomUUID(), UUID.randomUUID(), null, DeviceCommandType.WAKE,
                com.claircore.device.domain.model.valueobjects.DeviceCommandStatus.SENT,
                "{}", null, null, null, null, null);
        when(service.handle(any(ClaimPendingEdgeCommandsQuery.class)))
                .thenReturn(List.of(new EdgeCommandService.PendingEdgeCommand(command, "HW-0001")));

        assertThat(controller.pending(null, null, 10).getFirst().issuedAt()).isEmpty();
    }

    @Test
    void passesTheHardwareFilterAndWindowThroughToTheQuery() {
        when(service.handle(any(ClaimPendingEdgeCommandsQuery.class))).thenReturn(List.of());

        controller.pending("HW-0001", "2026-05-16T22:30:00Z", 50);

        var captor = ArgumentCaptor.forClass(ClaimPendingEdgeCommandsQuery.class);
        verify(service).handle(captor.capture());
        assertThat(captor.getValue().hardwareId()).isEqualTo("HW-0001");
        assertThat(captor.getValue().since()).isEqualTo(Instant.parse("2026-05-16T22:30:00Z"));
        assertThat(captor.getValue().limit()).isEqualTo(50);
    }

    @Test
    void rejectsALimitOutsideTheAcceptedRange() {
        assertThatThrownBy(() -> controller.pending(null, null, 0))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> controller.pending(null, null, 501))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void mapsEachOutcomeToItsStatusCode() {
        var body = new EdgeCommandAckRequest("HW-0001", EdgeCommandAckRequest.Result.OK, null);

        when(service.handle(any(com.claircore.device.domain.model.commands.AcknowledgeEdgeCommandCommand.class)))
                .thenReturn(EdgeCommandService.AcknowledgementOutcome.OK,
                        EdgeCommandService.AcknowledgementOutcome.CONFLICT,
                        EdgeCommandService.AcknowledgementOutcome.NOT_FOUND);

        assertThat(controller.acknowledge(UUID.randomUUID(), body).getStatusCode().value()).isEqualTo(200);
        assertThat(controller.acknowledge(UUID.randomUUID(), body).getStatusCode().value()).isEqualTo(409);
        assertThat(controller.acknowledge(UUID.randomUUID(), body).getStatusCode().value()).isEqualTo(404);
    }

    @Test
    void translatesTheWireResultIntoTheDomainResult() {
        when(service.handle(any(com.claircore.device.domain.model.commands.AcknowledgeEdgeCommandCommand.class)))
                .thenReturn(EdgeCommandService.AcknowledgementOutcome.OK);

        controller.acknowledge(UUID.randomUUID(),
                new EdgeCommandAckRequest("HW-0001", EdgeCommandAckRequest.Result.FAILED, "radio down"));

        var captor = ArgumentCaptor.forClass(
                com.claircore.device.domain.model.commands.AcknowledgeEdgeCommandCommand.class);
        verify(service).handle(captor.capture());
        assertThat(captor.getValue().result()).isEqualTo(EdgeCommandResult.FAILED);
        assertThat(captor.getValue().detail()).isEqualTo("radio down");
        assertThat(captor.getValue().hardwareId()).isEqualTo("HW-0001");
    }
}
