package com.claircore.alerting.application.internal.outboundservices.acl;

import com.claircore.alerting.domain.model.events.AlertIncidentChangedEvent;
import com.claircore.alerting.interfaces.events.AlertIncidentChangedIntegrationEvent;
import com.claircore.shared.infrastructure.edge.EdgeEventPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Service
public class AlertIncidentsChangedPublisher {

    private static final Logger LOGGER = LoggerFactory.getLogger(AlertIncidentsChangedPublisher.class);

    private final ApplicationEventPublisher eventPublisher;
    private final EdgeEventPublisher edgeEventPublisher;

    public AlertIncidentsChangedPublisher(
            ApplicationEventPublisher eventPublisher,
            EdgeEventPublisher edgeEventPublisher
    ) {
        this.eventPublisher = eventPublisher;
        this.edgeEventPublisher = edgeEventPublisher;
    }

    public void publish(AlertIncidentChangedIntegrationEvent event) {
        LOGGER.info("Publishing alert incident change {} (status={})", event.alertId(), event.status());
        publishAfterCommit(() -> {
            // Publish internal Spring event for local monolith communication (e.g. Notifications BC).
            eventPublisher.publishEvent(new AlertIncidentChangedEvent(
                    event.alertId(),
                    event.deviceId(),
                    event.hardwareId(),
                    event.spaceId(),
                    event.metric(),
                    event.thresholdValue(),
                    event.actualValue(),
                    event.message(),
                    event.status(),
                    event.occurredAt(),
                    event.resolvedAt()
            ));

            // Publish external Edge event via HTTPS.
            edgeEventPublisher.notifyChange("alert", event.alertId().toString());
        });
    }

    private void publishAfterCommit(Runnable notification) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()
                || !TransactionSynchronizationManager.isActualTransactionActive()) {
            notification.run();
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                notification.run();
            }
        });
    }
}
