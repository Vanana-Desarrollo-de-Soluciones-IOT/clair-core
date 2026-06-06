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
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeviceCommandsPendingKafkaPublisherTest {

    @Mock
    private OutboxMessageRepository outboxMessageRepository;

    @Test
    void shouldPersistOutboxMessageWhenPublishingCommandEvent() {
        DeviceCommandsPendingKafkaPublisher publisher = new DeviceCommandsPendingKafkaPublisher(outboxMessageRepository, new ObjectMapper());
        DeviceCommandIssuedIntegrationEvent event = new DeviceCommandIssuedIntegrationEvent(
                "cmd-1", "dev-1", "CLAIR-0KBG", "WAKE", "{}", "2026-06-05T13:00:00Z"
        );

        publisher.publish(event);

        ArgumentCaptor<OutboxMessage> captor = ArgumentCaptor.forClass(OutboxMessage.class);
        verify(outboxMessageRepository).save(captor.capture());
        assertEquals("clair.device.commands.pending", captor.getValue().getTopic());
        assertEquals("dev-1", captor.getValue().getMessageKey());
    }
}
