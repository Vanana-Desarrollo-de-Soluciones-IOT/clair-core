package com.claircore.alerting.application.internal.outboundservices.acl;

import com.claircore.alerting.domain.model.events.AlertIncidentChangedEvent;
import com.claircore.alerting.domain.model.valueobjects.AlertStatus;
import com.claircore.alerting.domain.model.valueobjects.MetricType;
import com.claircore.shared.infrastructure.edge.EdgeEventPublisher;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AlertIncidentsChangedPublisherTest {

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private EdgeEventPublisher edgeEventPublisher;

    @InjectMocks
    private AlertIncidentsChangedPublisher publisher;

    @AfterEach
    void clearTransactionSynchronization() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
        TransactionSynchronizationManager.setActualTransactionActive(false);
    }

    @Test
    void shouldPublishBothNotificationsAfterCommit() {
        AlertIncidentChangedIntegrationEvent event = event();
        TransactionSynchronizationManager.initSynchronization();
        TransactionSynchronizationManager.setActualTransactionActive(true);

        publisher.publish(event);

        verify(eventPublisher, never()).publishEvent(org.mockito.ArgumentMatchers.any());
        verify(edgeEventPublisher, never()).notifyChange("alert", event.alertId().toString());
        TransactionSynchronizationManager.getSynchronizations().forEach(sync -> sync.afterCommit());
        verify(eventPublisher).publishEvent(org.mockito.ArgumentMatchers.any(AlertIncidentChangedEvent.class));
        verify(edgeEventPublisher).notifyChange("alert", event.alertId().toString());
    }

    @Test
    void shouldNotPublishWhenTransactionRollsBack() {
        AlertIncidentChangedIntegrationEvent event = event();
        TransactionSynchronizationManager.initSynchronization();
        TransactionSynchronizationManager.setActualTransactionActive(true);

        publisher.publish(event);

        TransactionSynchronizationManager.getSynchronizations().forEach(sync -> sync.afterCompletion(
                org.springframework.transaction.support.TransactionSynchronization.STATUS_ROLLED_BACK));
        verify(eventPublisher, never()).publishEvent(org.mockito.ArgumentMatchers.any());
        verify(edgeEventPublisher, never()).notifyChange("alert", event.alertId().toString());
    }

    @Test
    void shouldPublishImmediatelyWhenThereIsNoTransaction() {
        AlertIncidentChangedIntegrationEvent event = event();

        publisher.publish(event);

        verify(eventPublisher).publishEvent(org.mockito.ArgumentMatchers.any(AlertIncidentChangedEvent.class));
        verify(edgeEventPublisher).notifyChange("alert", event.alertId().toString());
    }

    private AlertIncidentChangedIntegrationEvent event() {
        return new AlertIncidentChangedIntegrationEvent(
                UUID.randomUUID(), UUID.randomUUID(), "HW-01", UUID.randomUUID(), MetricType.CO2,
                BigDecimal.valueOf(800), BigDecimal.valueOf(900), "Too high", AlertStatus.ACTIVE,
                Instant.parse("2026-05-16T22:30:00Z"), null);
    }
}
