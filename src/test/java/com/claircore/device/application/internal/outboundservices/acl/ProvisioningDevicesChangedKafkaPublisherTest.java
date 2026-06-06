package com.claircore.device.application.internal.outboundservices.acl;

import com.claircore.shared.infrastructure.persistence.jpa.outbox.OutboxMessage;
import com.claircore.shared.infrastructure.persistence.jpa.outbox.OutboxMessageRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ProvisioningDevicesChangedKafkaPublisherTest {

    @Mock
    private OutboxMessageRepository outboxMessageRepository;

    @Test
    void shouldPersistOutboxMessageWhenPublishingDeviceChangedEvent() {
        ProvisioningDevicesChangedKafkaPublisher publisher = new ProvisioningDevicesChangedKafkaPublisher(outboxMessageRepository, new ObjectMapper());
        DeviceChangedIntegrationEvent event = new DeviceChangedIntegrationEvent(
                "dev-1", "CLAIR-0KBG", "api-key", "OFFLINE", "UPDATED", "2026-06-05T13:00:00Z"
        );

        publisher.publish(event);

        ArgumentCaptor<OutboxMessage> captor = ArgumentCaptor.forClass(OutboxMessage.class);
        verify(outboxMessageRepository).save(captor.capture());
        assertEquals("clair.provisioning.devices.changed", captor.getValue().getTopic());
        assertEquals("CLAIR-0KBG", captor.getValue().getMessageKey());
    }
}
