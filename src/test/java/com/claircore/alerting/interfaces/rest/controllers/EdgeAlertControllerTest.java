package com.claircore.alerting.interfaces.rest.controllers;

import com.claircore.alerting.domain.model.entities.Alert;
import com.claircore.alerting.domain.model.valueobjects.AlertStatus;
import com.claircore.alerting.infrastructure.persistence.jpa.repositories.AlertRepository;
import com.claircore.alerting.interfaces.rest.resources.EdgeAlertAckRequest;
import org.junit.jupiter.api.Test;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class EdgeAlertControllerTest {
    @Test void rejectsAckForWrongHardware() {
        var repository = mock(AlertRepository.class); var alert = mock(Alert.class); UUID id = UUID.randomUUID();
        when(repository.findById(id)).thenReturn(Optional.of(alert)); when(alert.getStatus()).thenReturn(AlertStatus.ACTIVE);
        when(repository.findHardwareIdByAlertId(id)).thenReturn(Optional.of("HW-0001"));
        var response = new EdgeAlertController(repository).acknowledge(id, new EdgeAlertAckRequest("HW-0002", Instant.now()));
        assertEquals(404, response.getStatusCode().value()); verify(alert, never()).acknowledge();
    }
    @Test void returnsConflictForIdempotentAck() {
        var repository = mock(AlertRepository.class); var alert = mock(Alert.class); UUID id = UUID.randomUUID();
        when(repository.findById(id)).thenReturn(Optional.of(alert)); when(alert.getStatus()).thenReturn(AlertStatus.ACKNOWLEDGED);
        assertEquals(409, new EdgeAlertController(repository).acknowledge(id, new EdgeAlertAckRequest("HW-0001", Instant.now())).getStatusCode().value());
    }
}
