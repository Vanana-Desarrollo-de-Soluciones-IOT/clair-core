package com.claircore.shared.application;

import com.claircore.alerting.application.internal.outboundservices.edge.AlertIncidentsChangedPublisher;
import com.claircore.alerting.interfaces.events.AlertIncidentChangedIntegrationEvent;
import com.claircore.billing.application.internal.commandservices.UserPlanCommandServiceImpl;
import com.claircore.billing.application.internal.eventhandlers.UserRegisteredEventHandler;
import com.claircore.billing.domain.model.valueobjects.UserId;
import com.claircore.billing.domain.repositories.UserPlanRepository;
import com.claircore.billing.infrastructure.persistence.jpa.adapters.UserPlanRepositoryImpl;
import com.claircore.iam.interfaces.events.UserRegisteredIntegrationEvent;
import com.claircore.notifications.application.internal.commandservices.PushNotificationCommandServiceImpl;
import com.claircore.notifications.application.internal.eventhandlers.AlertIncidentChangedEventHandler;
import com.claircore.notifications.application.internal.outboundservices.acl.ExternalAlertingService;
import com.claircore.notifications.application.internal.outboundservices.acl.ExternalDeviceService;
import com.claircore.notifications.application.internal.outboundservices.push.PushNotificationDeliveryService;
import com.claircore.notifications.domain.repositories.PushNotificationLogRepository;
import com.claircore.notifications.infrastructure.persistence.jpa.adapters.PushNotificationLogRepositoryImpl;
import com.claircore.alerting.interfaces.acl.AlertDetails;
import com.claircore.shared.application.outboundservices.EdgeNotifier;
import com.claircore.shared.infrastructure.config.JpaAuditingConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@DataJpaTest
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@Import({JpaAuditingConfiguration.class, AlertIncidentsChangedPublisher.class,
        AlertIncidentChangedEventHandler.class, PushNotificationCommandServiceImpl.class,
        PushNotificationLogRepositoryImpl.class, UserRegisteredEventHandler.class,
        UserPlanCommandServiceImpl.class, UserPlanRepositoryImpl.class,
        com.claircore.alerting.application.internal.eventhandlers.TelemetryRecordedEventHandler.class,
        com.claircore.analytics.application.internal.eventhandlers.TelemetryRecordedEventHandler.class})
class CommittedEventsIntegrationTest {
    @Autowired PlatformTransactionManager transactions;
    @Autowired AlertIncidentsChangedPublisher incidents;
    @Autowired ApplicationEventPublisher events;
    @Autowired PushNotificationLogRepository logs;
    @Autowired UserPlanRepository plans;
    @MockitoBean com.claircore.alerting.application.commandservices.AlertCommandService alertCommands;
    @MockitoBean com.claircore.analytics.application.commandservices.KpiLiveMetricsCommandService analyticsCommands;
    @MockitoBean EdgeNotifier edge;
    @MockitoBean ExternalDeviceService devices;
    @MockitoBean ExternalAlertingService alerts;
    @MockitoBean PushNotificationDeliveryService delivery;

    @Test void sentPushHistoryIsCommittedAfterTheAlertTransaction() {
        UUID owner = UUID.randomUUID();
        var event = event();
        arrange(event, owner);
        new TransactionTemplate(transactions).executeWithoutResult(status -> {
            incidents.publish(event);
            verifyNoInteractions(delivery);
        });
        assertThat(logs.findByUserId(owner, 0, 10).items()).singleElement()
                .satisfies(log -> assertThat(log.isSent()).isTrue());
        verify(delivery).sendPushNotification(eq(owner), anyString(), anyString());
    }

    @Test void failedPushHistoryIsAlsoCommittedAfterTheAlertTransaction() {
        UUID owner = UUID.randomUUID();
        var event = event();
        arrange(event, owner);
        doThrow(new IllegalStateException("delivery unavailable")).when(delivery)
                .sendPushNotification(any(), anyString(), anyString());
        new TransactionTemplate(transactions).executeWithoutResult(status -> incidents.publish(event));
        assertThat(logs.findByUserId(owner, 0, 10).items()).singleElement().satisfies(log -> {
            assertThat(log.isSent()).isFalse();
            assertThat(log.getErrorMessage()).isEqualTo("delivery unavailable");
        });
    }

    @Test void rolledBackAlertsDoNotSendPushesOrHints() {
        new TransactionTemplate(transactions).executeWithoutResult(status -> {
            incidents.publish(event());
            status.setRollbackOnly();
        });
        verifyNoInteractions(delivery, edge, devices, alerts);
    }

    @Test void registrationCreatesThePlanOnlyAfterCommit() {
        var id = new UserId(UUID.randomUUID());
        new TransactionTemplate(transactions).executeWithoutResult(status -> {
            events.publishEvent(new UserRegisteredIntegrationEvent(id.userId()));
            assertThat(plans.findByUserId(id)).isEmpty();
        });
        assertThat(plans.findByUserId(id)).isPresent();
    }

    @Test void rolledBackRegistrationDoesNotCreateAPlan() {
        var id = new UserId(UUID.randomUUID());
        new TransactionTemplate(transactions).executeWithoutResult(status -> {
            events.publishEvent(new UserRegisteredIntegrationEvent(id.userId()));
            status.setRollbackOnly();
        });
        assertThat(plans.findByUserId(id)).isEmpty();
    }

    @Test void telemetryConsumersRunOnlyForCommittedReadings() {
        var event = new com.claircore.evaluation.interfaces.events.TelemetryRecordedIntegrationEvent(
                UUID.randomUUID(), 800, 24, 50, 5, 10, 15, "ONLINE", "wifi", -50,
                "PE", 1, "OK", 100, "12:00", Instant.now());
        new TransactionTemplate(transactions).executeWithoutResult(status -> {
            events.publishEvent(event);
            verifyNoInteractions(alertCommands, analyticsCommands);
            status.setRollbackOnly();
        });
        verifyNoInteractions(alertCommands, analyticsCommands);
        new TransactionTemplate(transactions).executeWithoutResult(status -> {
            events.publishEvent(event);
            verifyNoInteractions(alertCommands, analyticsCommands);
        });
        verify(alertCommands).handle(any(com.claircore.alerting.domain.model.commands.EvaluateTelemetryForAlertsCommand.class));
        verify(analyticsCommands).handle(any(com.claircore.analytics.domain.model.commands.ProcessTelemetryAnalyticCommand.class));
    }

    private void arrange(AlertIncidentChangedIntegrationEvent event, UUID owner) {
        when(devices.fetchOwnerIdByDeviceId(event.deviceId())).thenReturn(Optional.of(owner));
        when(devices.fetchDeviceNameByDeviceId(event.deviceId())).thenReturn(Optional.of("Sensor"));
        when(alerts.fetchAlertDetailsById(event.alertId())).thenReturn(Optional.of(new AlertDetails(
                event.alertId(), event.deviceId(), event.spaceId(), "Sensor", "CO2", BigDecimal.ONE,
                BigDecimal.TEN, "Too high", "ACTIVE", "WARNING", event.occurredAt())));
    }

    private AlertIncidentChangedIntegrationEvent event() {
        return new AlertIncidentChangedIntegrationEvent(UUID.randomUUID(), UUID.randomUUID(), "HW-0001",
                UUID.randomUUID(), "CO2", BigDecimal.ONE, BigDecimal.TEN, "Too high", "ACTIVE", Instant.now(), null);
    }
}
