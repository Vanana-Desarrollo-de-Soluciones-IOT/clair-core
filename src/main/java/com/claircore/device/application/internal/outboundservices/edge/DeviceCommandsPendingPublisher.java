package com.claircore.device.application.internal.outboundservices.edge;

import com.claircore.device.interfaces.events.DeviceCommandIssuedIntegrationEvent;

import com.claircore.shared.application.outboundservices.EdgeNotifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Service
public class DeviceCommandsPendingPublisher {

    private static final Logger LOGGER = LoggerFactory.getLogger(DeviceCommandsPendingPublisher.class);

    private final EdgeNotifier edgeEventPublisher;

    public DeviceCommandsPendingPublisher(EdgeNotifier edgeEventPublisher) {
        this.edgeEventPublisher = edgeEventPublisher;
    }

    public void publish(DeviceCommandIssuedIntegrationEvent event) {
        LOGGER.info("Publishing pending command {} to Edge for device {}", event.commandId(), event.deviceId());
        if (!TransactionSynchronizationManager.isSynchronizationActive()
                || !TransactionSynchronizationManager.isActualTransactionActive()) {
            edgeEventPublisher.notifyChange("command", event.commandId());
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                edgeEventPublisher.notifyChange("command", event.commandId());
            }
        });
    }
}
