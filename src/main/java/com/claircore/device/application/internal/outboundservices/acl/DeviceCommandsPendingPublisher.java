package com.claircore.device.application.internal.outboundservices.acl;

import com.claircore.shared.infrastructure.edge.EdgeEventPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class DeviceCommandsPendingPublisher {

    private static final Logger LOGGER = LoggerFactory.getLogger(DeviceCommandsPendingPublisher.class);

    private final EdgeEventPublisher edgeEventPublisher;

    public DeviceCommandsPendingPublisher(EdgeEventPublisher edgeEventPublisher) {
        this.edgeEventPublisher = edgeEventPublisher;
    }

    public void publish(DeviceCommandIssuedIntegrationEvent event) {
        LOGGER.info("Publishing pending command {} to Edge for device {}", event.commandId(), event.deviceId());
        edgeEventPublisher.notifyChange("command", event.commandId());
    }
}
