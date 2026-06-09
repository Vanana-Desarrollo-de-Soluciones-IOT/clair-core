package com.claircore.device.application.internal.outboundservices.acl;

import com.claircore.shared.infrastructure.edge.EdgeEventPublisher;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class DeviceCommandsPendingPublisherTest {

    @Mock
    private EdgeEventPublisher edgeEventPublisher;

    @InjectMocks
    private DeviceCommandsPendingPublisher publisher;

    @Test
    void shouldPublishCommandToEdgeWhenEventIsValid() {
        // Arrange
        DeviceCommandIssuedIntegrationEvent event = new DeviceCommandIssuedIntegrationEvent(
                "cmd-1",
                "dev-1",
                "HW-01",
                "WAKE",
                "{}",
                "2026-05-16T22:30:00Z"
        );

        // Act
        publisher.publish(event);

        // Assert
        verify(edgeEventPublisher).publishDeviceCommand(event);
    }
}
