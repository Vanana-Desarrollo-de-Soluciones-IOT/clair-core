package com.claircore.alerting.application.internal.outboundservices.edge;

import com.claircore.alerting.interfaces.events.AlertIncidentChangedIntegrationEvent;
import com.claircore.shared.application.outboundservices.EdgeNotifier;
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
    private final EdgeNotifier edgeEventPublisher;

    public AlertIncidentsChangedPublisher(
            ApplicationEventPublisher eventPublisher,
            EdgeNotifier edgeEventPublisher
    ) {
        this.eventPublisher = eventPublisher;
        this.edgeEventPublisher = edgeEventPublisher;
    }

    public void publish(AlertIncidentChangedIntegrationEvent event) {
        LOGGER.info("Publishing alert incident change {} (status={})", event.alertId(), event.status());
        publishAfterCommit(() -> {
            // Publish internal Spring event for local monolith communication (e.g. Notifications BC).
            eventPublisher.publishEvent(event);

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
