package com.claircore.device.application.internal.outboundservices.acl;

import com.claircore.shared.infrastructure.edge.EdgeEventPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class ProvisioningDevicesChangedPublisher {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProvisioningDevicesChangedPublisher.class);

    private final EdgeEventPublisher edgeEventPublisher;

    public ProvisioningDevicesChangedPublisher(EdgeEventPublisher edgeEventPublisher) {
        this.edgeEventPublisher = edgeEventPublisher;
    }

    public void publish(DeviceChangedIntegrationEvent event) {
        LOGGER.info("Publishing device changed event for device {} (type={})", event.deviceId(), event.changeType());
        edgeEventPublisher.publishDeviceChanged(event);
    }
}
