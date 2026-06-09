package com.claircore.notifications.application.internal.inboundservices.acl;

import com.claircore.alerting.domain.model.valueobjects.AlertStatus;
import com.claircore.alerting.domain.model.valueobjects.MetricType;
import com.claircore.alerting.domain.model.events.AlertIncidentChangedEvent;
import com.claircore.alerting.interfaces.acl.AlertDetails;
import com.claircore.notifications.application.internal.outboundservices.acl.ExternalAlertingService;
import com.claircore.notifications.application.internal.outboundservices.acl.ExternalDeviceService;
import com.claircore.notifications.domain.repositories.PushNotificationHistoryRepository;
import com.claircore.notifications.domain.services.PushNotificationDeliveryService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AlertIncidentChangedEventListenerTest {

    @Mock
    private ExternalAlertingService externalAlertingService;

    @Mock
    private ExternalDeviceService externalDeviceService;

    @Mock
    private PushNotificationDeliveryService pushNotificationDeliveryService;

    @Mock
    private PushNotificationHistoryRepository pushNotificationHistoryRepository;

    @InjectMocks
    private AlertIncidentChangedEventListener listener;

    @Test
    void shouldSendPushNotificationWhenAlertIsActiveAndOwnerExists() {
        // Arrange
        UUID alertId = UUID.randomUUID();
        UUID deviceId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        Instant now = Instant.now();

        AlertIncidentChangedEvent event = new AlertIncidentChangedEvent(
                alertId,
                deviceId,
                "HW-01",
                UUID.randomUUID(),
                MetricType.CO2,
                BigDecimal.valueOf(800.0),
                BigDecimal.valueOf(1000.0),
                "CO2 threshold exceeded",
                AlertStatus.ACTIVE,
                now,
                null
        );

        AlertDetails alertDetails = new AlertDetails(
                alertId,
                deviceId,
                UUID.randomUUID(),
                "Device 1",
                "CO2",
                BigDecimal.valueOf(800.0),
                BigDecimal.valueOf(1000.0),
                "CO2 threshold exceeded",
                "ACTIVE",
                "CRITICAL",
                now
        );

        when(externalDeviceService.fetchOwnerIdByDeviceId(deviceId)).thenReturn(Optional.of(ownerId));
        when(externalAlertingService.fetchAlertDetailsById(alertId)).thenReturn(Optional.of(alertDetails));
        when(externalDeviceService.fetchDeviceNameByDeviceId(deviceId)).thenReturn(Optional.of("Device 1"));

        // Act
        listener.onAlertIncidentChanged(event);

        // Assert
        verify(pushNotificationDeliveryService).sendPushNotification(
                eq(ownerId),
                eq("Active Alert: Device 1"),
                eq("[Device 1] CO2 threshold exceeded")
        );
        verify(pushNotificationHistoryRepository).save(any());
    }
}
