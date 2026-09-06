package com.claircore.alerting.interfaces.rest.controllers;

import com.claircore.alerting.application.commandservices.AlertCommandService;
import com.claircore.alerting.application.queryservices.AlertQueryService;
import com.claircore.alerting.domain.model.aggregates.Alert;
import com.claircore.alerting.domain.model.commands.AcknowledgeEdgeAlertCommand;
import com.claircore.alerting.domain.model.queries.GetPendingEdgeAlertsQuery;
import com.claircore.alerting.domain.model.valueobjects.AlertSeverity;
import com.claircore.alerting.domain.model.valueobjects.MetricType;
import com.claircore.alerting.interfaces.rest.resources.EdgeAlertAckRequest;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class EdgeAlertControllerTest {

    private final AlertQueryService queryService = mock(AlertQueryService.class);
    private final AlertCommandService commandService = mock(AlertCommandService.class);
    private final EdgeAlertController controller = new EdgeAlertController(queryService, commandService);

    @Test
    void pendingIncludesResolvedAlertsForEdgeDelivery() {
        var occurredAt = Instant.parse("2024-01-01T00:00:00Z");
        var resolvedAt = Instant.parse("2024-01-01T00:05:00Z");
        var alert = alert(occurredAt);
        alert.resolve(resolvedAt);
        when(queryService.fetchPendingForEdge(new GetPendingEdgeAlertsQuery(null, 200)))
                .thenReturn(List.of(new AlertQueryService.PendingEdgeAlert(alert, "HW-0001")));

        var result = controller.pending(null, 200);

        assertThat(result).singleElement().satisfies(resource -> {
            assertThat(resource.status()).isEqualTo("RESOLVED");
            assertThat(resource.resolvedAt()).isEqualTo(resolvedAt.toString());
            assertThat(resource.hardwareId()).isEqualTo("HW-0001");
            assertThat(resource.alertId()).isEqualTo(alert.getId().toString());
        });
    }

    @Test
    void rejectsALimitOutsideTheAllowedRange() {
        assertThrows(IllegalArgumentException.class, () -> controller.pending(null, 501));
    }

    @Test
    void mapsTheAcknowledgementOutcomeOntoAStatusCode() {
        UUID alertId = UUID.randomUUID();
        var request = new EdgeAlertAckRequest("HW-0001", Instant.parse("2024-01-01T00:00:00Z"));

        when(commandService.handle(any(AcknowledgeEdgeAlertCommand.class)))
                .thenReturn(AlertCommandService.AcknowledgementOutcome.OK);
        assertThat(controller.acknowledge(alertId, request).getStatusCode().value()).isEqualTo(200);

        when(commandService.handle(any(AcknowledgeEdgeAlertCommand.class)))
                .thenReturn(AlertCommandService.AcknowledgementOutcome.CONFLICT);
        assertThat(controller.acknowledge(alertId, request).getStatusCode().value()).isEqualTo(409);

        when(commandService.handle(any(AcknowledgeEdgeAlertCommand.class)))
                .thenReturn(AlertCommandService.AcknowledgementOutcome.NOT_FOUND);
        assertThat(controller.acknowledge(alertId, request).getStatusCode().value()).isEqualTo(404);

        verify(commandService, org.mockito.Mockito.times(3))
                .handle(new AcknowledgeEdgeAlertCommand(alertId, "HW-0001", Instant.parse("2024-01-01T00:00:00Z")));
    }

    private static Alert alert(Instant occurredAt) {
        return new Alert(UUID.randomUUID(), UUID.randomUUID(), "Floor 2", "Living Room",
                MetricType.CO2, new BigDecimal("800.00"), new BigDecimal("900.00"),
                "CO2 threshold exceeded", AlertSeverity.CRITICAL, occurredAt);
    }
}
