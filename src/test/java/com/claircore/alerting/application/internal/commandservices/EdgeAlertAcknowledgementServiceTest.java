package com.claircore.alerting.application.internal.commandservices;

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

class EdgeAlertAcknowledgementServiceTest {
    @Test
    void usesLockedLookupAndTransitionsActiveAlert() {
        AlertRepository repository = mock(AlertRepository.class);
        Alert alert = mock(Alert.class);
        UUID id = UUID.randomUUID();
        when(repository.findByIdForAcknowledgement(id)).thenReturn(Optional.of(alert));
        when(alert.getStatus()).thenReturn(AlertStatus.ACTIVE);
        when(alert.getId()).thenReturn(id);
        when(repository.findHardwareIdByAlertId(id)).thenReturn(Optional.of("HW-1"));

        assertEquals(EdgeAlertAcknowledgementService.Outcome.OK,
                new EdgeAlertAcknowledgementService(repository).acknowledge(
                        id, new EdgeAlertAckRequest("HW-1", Instant.now())));
        verify(alert).acknowledge();
        verify(repository).save(alert);
        verify(repository, never()).findById(id);
    }

    @Test
    void concurrentAlreadyAcknowledgedStateIsConflict() {
        AlertRepository repository = mock(AlertRepository.class);
        Alert alert = mock(Alert.class);
        UUID id = UUID.randomUUID();
        when(repository.findByIdForAcknowledgement(id)).thenReturn(Optional.of(alert));
        when(alert.getStatus()).thenReturn(AlertStatus.ACKNOWLEDGED);
        when(alert.getId()).thenReturn(id);
        when(repository.findHardwareIdByAlertId(id)).thenReturn(Optional.of("HW-1"));

        assertEquals(EdgeAlertAcknowledgementService.Outcome.CONFLICT,
                new EdgeAlertAcknowledgementService(repository).acknowledge(
                        id, new EdgeAlertAckRequest("HW-1", Instant.now())));
        verify(alert, never()).acknowledge();
        verify(repository, never()).save(any());
    }

    @Test
    void wrongHardwareReturnsNotFoundBeforeCheckingTerminalState() {
        AlertRepository repository = mock(AlertRepository.class);
        Alert alert = mock(Alert.class);
        UUID id = UUID.randomUUID();
        when(repository.findByIdForAcknowledgement(id)).thenReturn(Optional.of(alert));
        when(alert.getId()).thenReturn(id);
        when(alert.getStatus()).thenReturn(AlertStatus.ACKNOWLEDGED);
        when(repository.findHardwareIdByAlertId(id)).thenReturn(Optional.of("HW-owner"));

        assertEquals(EdgeAlertAcknowledgementService.Outcome.NOT_FOUND,
                new EdgeAlertAcknowledgementService(repository).acknowledge(
                        id, new EdgeAlertAckRequest("HW-other", Instant.now())));
        verify(alert, never()).getStatus();
        verify(alert, never()).acknowledge();
        verify(repository, never()).save(any());
    }
}
