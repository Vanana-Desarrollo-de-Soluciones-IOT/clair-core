package com.claircore.notifications.application.internal.inboundservices.acl;

import com.claircore.alerting.application.internal.outboundservices.acl.AlertIncidentChangedIntegrationEvent;
import com.claircore.alerting.domain.model.valueobjects.AlertStatus;
import com.claircore.alerting.interfaces.acl.AlertDetails;
import com.claircore.notifications.application.internal.outboundservices.acl.ExternalAlertingService;
import com.claircore.notifications.application.internal.outboundservices.acl.ExternalDeviceService;
import com.claircore.notifications.domain.model.entities.PushNotificationLog;
import com.claircore.notifications.domain.repositories.PushNotificationHistoryRepository;
import com.claircore.notifications.domain.services.PushNotificationDeliveryService;
import com.claircore.shared.infrastructure.kafka.KafkaInboxService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AlertIncidentChangedKafkaConsumerTest {

    @Mock
    private ExternalAlertingService externalAlertingService;

    @Mock
    private ExternalDeviceService externalDeviceService;

    @Mock
    private PushNotificationDeliveryService pushNotificationDeliveryService;

    @Mock
    private PushNotificationHistoryRepository pushNotificationHistoryRepository;

    @Mock
    private KafkaInboxService kafkaInboxService;

    private AlertIncidentChangedKafkaConsumer consumer;

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules()
            .copy()
            .setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        consumer = new AlertIncidentChangedKafkaConsumer(
                externalAlertingService,
                externalDeviceService,
                pushNotificationDeliveryService,
                pushNotificationHistoryRepository,
                kafkaInboxService,
                objectMapper
        );
    }

    @Test
    void shouldSendPushNotificationWhenAlertHasOwnerAndDetails() throws Exception {
        UUID alertId = UUID.randomUUID();
        UUID deviceId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        var event = new AlertIncidentChangedIntegrationEvent(
                alertId,
                deviceId,
                "HW-1",
                UUID.randomUUID(),
                null,
                BigDecimal.TEN,
                BigDecimal.ONE,
                "High alert",
                AlertStatus.ACTIVE,
                Instant.now(),
                null
        );
        var details = new AlertDetails(
                alertId,
                deviceId,
                UUID.randomUUID(),
                "Kitchen sensor",
                "PM25",
                BigDecimal.TEN,
                BigDecimal.ONE,
                "High alert",
                "ACTIVE",
                "CRITICAL",
                Instant.now()
        );

        when(kafkaInboxService.shouldProcess(any(), any(), anyInt(), anyLong())).thenReturn(true);
        when(externalDeviceService.fetchOwnerIdByDeviceId(deviceId)).thenReturn(Optional.of(userId));
        when(externalAlertingService.fetchAlertDetailsById(alertId)).thenReturn(Optional.of(details));

        consumer.consume(new ConsumerRecord<>("clair.device.alert.incident.changed", 0, 0, "key", objectMapper.writeValueAsString(event)));

        ArgumentCaptor<PushNotificationLog> captor = ArgumentCaptor.forClass(PushNotificationLog.class);
        verify(pushNotificationHistoryRepository).save(captor.capture());
        assertEquals(userId, captor.getValue().getUserId());
        verify(kafkaInboxService).markProcessed(any(), any(), anyInt(), anyLong());
    }

    @Test
    void shouldSkipPushNotificationWhenNoOwnerIsFound() throws Exception {
        UUID alertId = UUID.randomUUID();
        UUID deviceId = UUID.randomUUID();
        var event = new AlertIncidentChangedIntegrationEvent(
                alertId,
                deviceId,
                "HW-1",
                UUID.randomUUID(),
                null,
                BigDecimal.TEN,
                BigDecimal.ONE,
                "High alert",
                AlertStatus.ACTIVE,
                Instant.now(),
                null
        );

        when(kafkaInboxService.shouldProcess(any(), any(), anyInt(), anyLong())).thenReturn(true);
        when(externalDeviceService.fetchOwnerIdByDeviceId(deviceId)).thenReturn(Optional.empty());

        consumer.consume(new ConsumerRecord<>("clair.device.alert.incident.changed", 0, 0, "key", objectMapper.writeValueAsString(event)));

        verify(pushNotificationDeliveryService, never()).sendPushNotification(any(), any(), any());
        verify(pushNotificationHistoryRepository, never()).save(any());
        verify(kafkaInboxService).markProcessed(any(), any(), anyInt(), anyLong());
    }
}
