package com.claircore.alerting.interfaces.rest.controllers;

import com.claircore.alerting.domain.model.entities.Alert;
import com.claircore.alerting.domain.model.valueobjects.AlertStatus;
import com.claircore.alerting.domain.model.valueobjects.MetricType;
import com.claircore.alerting.application.internal.commandservices.EdgeAlertAcknowledgementService;
import com.claircore.alerting.infrastructure.persistence.jpa.repositories.AlertRepository;
import com.claircore.alerting.interfaces.rest.resources.EdgeAlertAckRequest;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class EdgeAlertControllerTest {
    @Test void pendingIncludesResolvedAlertsForEdgeDelivery() {
        var repository = mock(AlertRepository.class);
        var projection = mock(AlertRepository.EdgeAlertProjection.class);
        var alert = mock(Alert.class);
        var alertId = UUID.randomUUID();
        var deviceId = UUID.randomUUID();
        var occurredAt = Instant.parse("2024-01-01T00:00:00Z");
        var resolvedAt = Instant.parse("2024-01-01T00:05:00Z");
        var statuses = List.of(AlertStatus.ACTIVE, AlertStatus.RESOLVED);
        var pageable = PageRequest.of(0, 200);

        when(projection.getAlert()).thenReturn(alert);
        when(projection.getHardwareId()).thenReturn("HW-0001");
        when(alert.getId()).thenReturn(alertId);
        when(alert.getDeviceId()).thenReturn(deviceId);
        when(alert.getMetric()).thenReturn(MetricType.CO2);
        when(alert.getStatus()).thenReturn(AlertStatus.RESOLVED);
        when(alert.getOccurredAt()).thenReturn(occurredAt);
        when(alert.getResolvedAt()).thenReturn(resolvedAt);
        when(repository.findPendingForEdge(statuses, null, pageable)).thenReturn(List.of(projection));

        var result = new EdgeAlertController(
                repository, new EdgeAlertAcknowledgementService(repository)
        ).pending(null, 200);

        assertEquals(1, result.size());
        assertEquals("RESOLVED", result.getFirst().get("status"));
        assertEquals(resolvedAt.toString(), result.getFirst().get("resolved_at"));
        verify(repository).findPendingForEdge(statuses, null, pageable);
    }

    @Test void rejectsAckForWrongHardware() {
        var repository = mock(AlertRepository.class); var alert = mock(Alert.class); UUID id = UUID.randomUUID();
        when(repository.findByIdForAcknowledgement(id)).thenReturn(Optional.of(alert)); when(alert.getStatus()).thenReturn(AlertStatus.ACTIVE);
        when(repository.findHardwareIdByAlertId(id)).thenReturn(Optional.of("HW-0001"));
        var response = new EdgeAlertController(repository, new EdgeAlertAcknowledgementService(repository)).acknowledge(id, new EdgeAlertAckRequest("HW-0002", Instant.now()));
        assertEquals(404, response.getStatusCode().value()); verify(alert, never()).acknowledge();
    }
    @Test void returnsConflictForIdempotentAck() {
        var repository = mock(AlertRepository.class); var alert = mock(Alert.class); UUID id = UUID.randomUUID();
        when(repository.findByIdForAcknowledgement(id)).thenReturn(Optional.of(alert)); when(alert.getStatus()).thenReturn(AlertStatus.ACKNOWLEDGED);
        when(alert.getId()).thenReturn(id); when(repository.findHardwareIdByAlertId(id)).thenReturn(Optional.of("HW-0001"));
        assertEquals(409, new EdgeAlertController(repository, new EdgeAlertAcknowledgementService(repository)).acknowledge(id, new EdgeAlertAckRequest("HW-0001", Instant.now())).getStatusCode().value());
    }
}
