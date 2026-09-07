package com.claircore.device.application.internal.outboundservices.edge;

import com.claircore.device.interfaces.events.DeviceChangedIntegrationEvent;

import com.claircore.shared.application.outboundservices.EdgeNotifier;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ProvisioningDevicesChangedPublisherTest {

    @Mock
    private EdgeNotifier edgeEventPublisher;

    @InjectMocks
    private ProvisioningDevicesChangedPublisher publisher;

    @Test
    void shouldNotifyEdgeWithoutSendingDevicePayload() {
        // Arrange
        DeviceChangedIntegrationEvent event = new DeviceChangedIntegrationEvent(
                "dev-1",
                "HW-01",
                "api-key-1",
                "ONLINE",
                "UPDATED",
                "2026-05-16T22:30:00Z"
        );

        // Act
        publisher.publish(event);

        // Assert
        verify(edgeEventPublisher).notifyChange("device", "dev-1");
    }
}
