package com.claircore.device.application.internal.outboundservices.edge;

import com.claircore.device.interfaces.events.DeviceChangedIntegrationEvent;

import com.claircore.shared.application.outboundservices.EdgeNotifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Service
public class ProvisioningDevicesChangedPublisher {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProvisioningDevicesChangedPublisher.class);

    private final EdgeNotifier edgeEventPublisher;

    public ProvisioningDevicesChangedPublisher(EdgeNotifier edgeEventPublisher) {
        this.edgeEventPublisher = edgeEventPublisher;
    }

    public void publish(DeviceChangedIntegrationEvent event) {
        LOGGER.info("Publishing device changed event for device {} (type={})", event.deviceId(), event.changeType());
        publishAfterCommit(() -> edgeEventPublisher.notifyChange("device", event.deviceId()));
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
