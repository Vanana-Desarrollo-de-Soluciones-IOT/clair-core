package com.claircore.alerting.application.internal.commandservices;

import com.claircore.alerting.application.commandservices.AlertCommandService.AcknowledgementOutcome;
import com.claircore.alerting.application.internal.outboundservices.edge.AlertIncidentsChangedPublisher;
import com.claircore.alerting.application.internal.outboundservices.acl.ExternalAlertingDeviceService;
import com.claircore.alerting.application.internal.outboundservices.acl.ExternalAlertingThresholdService;
import com.claircore.alerting.domain.model.aggregates.Alert;
import com.claircore.alerting.application.commandservices.AlertCommandService;
import com.claircore.alerting.domain.model.commands.AcknowledgeEdgeAlertCommand;
import com.claircore.alerting.domain.model.commands.RecordEdgeAlertReceiptCommand;
import com.claircore.alerting.domain.model.commands.EvaluateTelemetryForAlertsCommand;
import com.claircore.alerting.domain.model.valueobjects.AlertSeverity;
import com.claircore.alerting.domain.model.valueobjects.AlertStatus;
import com.claircore.alerting.domain.model.valueobjects.MetricType;
import com.claircore.alerting.domain.repositories.AlertRepository;
import com.claircore.alerting.interfaces.events.AlertIncidentChangedIntegrationEvent;
import com.claircore.device.interfaces.acl.ThresholdSummary;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AlertCommandServiceImplTest {

    @Mock
    private AlertRepository alertRepository;

    @Mock
    private ExternalAlertingThresholdService externalThresholdService;

    @Mock
    private ExternalAlertingDeviceService externalDeviceService;

    @Mock
    private AlertIncidentsChangedPublisher alertIncidentsChangedPublisher;

    @InjectMocks
    private AlertCommandServiceImpl service;

    private static final UUID DEVICE_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final Instant OCCURRED_AT = Instant.parse("2026-05-16T22:30:00Z");

    /** The threshold arrives as a name, not device's enum, and alerting maps it to its own metric. */
    @Test
    void opensAnAlertWhenAThresholdIsBreached() {
        when(alertRepository.nextTransitionSequence()).thenReturn(41L);
        UUID spaceId = UUID.randomUUID();
        when(externalDeviceService.fetchSpaceIdByDeviceId(DEVICE_ID)).thenReturn(Optional.of(spaceId));
        when(externalThresholdService.fetchEnabledThresholdsByDeviceId(DEVICE_ID))
                .thenReturn(List.of(new ThresholdSummary("PM25", new BigDecimal("50.00"), true)));
        when(alertRepository.findFirstByDeviceIdAndMetricAndStatusIn(eq(DEVICE_ID), eq(MetricType.PM25), anyCollection()))
                .thenReturn(Optional.empty());
        when(externalDeviceService.fetchSpaceNameBySpaceId(spaceId)).thenReturn(Optional.of("Floor 2"));
        when(externalDeviceService.fetchDeviceNameByDeviceId(DEVICE_ID)).thenReturn(Optional.of("Living Room"));
        when(externalDeviceService.fetchHardwareIdByDeviceId(DEVICE_ID)).thenReturn(Optional.of("HW-0001"));
        when(alertRepository.save(any(Alert.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.handle(command(new BigDecimal("80.00")));

        ArgumentCaptor<Alert> saved = ArgumentCaptor.forClass(Alert.class);
        verify(alertRepository).save(saved.capture());
        assertThat(saved.getValue().getMetric()).isEqualTo(MetricType.PM25);
        assertThat(saved.getValue().getStatus()).isEqualTo(AlertStatus.ACTIVE);
        assertThat(saved.getValue().getSeverity()).isEqualTo(AlertSeverity.CRITICAL);
        assertThat(saved.getValue().getSpaceName()).isEqualTo("Floor 2");
        assertThat(saved.getValue().getTransitionSequence()).isEqualTo(41L);

        ArgumentCaptor<AlertIncidentChangedIntegrationEvent> published =
                ArgumentCaptor.forClass(AlertIncidentChangedIntegrationEvent.class);
        verify(alertIncidentsChangedPublisher).publish(published.capture());
        assertThat(published.getValue().hardwareId()).isEqualTo("HW-0001");
    }

    @Test
    void doesNotOpenASecondAlertWhileOneIsStillOpen() {
        when(externalDeviceService.fetchSpaceIdByDeviceId(DEVICE_ID)).thenReturn(Optional.empty());
        when(externalThresholdService.fetchEnabledThresholdsByDeviceId(DEVICE_ID))
                .thenReturn(List.of(new ThresholdSummary("PM25", new BigDecimal("50.00"), true)));
        when(alertRepository.findFirstByDeviceIdAndMetricAndStatusIn(eq(DEVICE_ID), eq(MetricType.PM25), anyCollection()))
                .thenReturn(Optional.of(openAlert()));

        service.handle(command(new BigDecimal("80.00")));

        verify(alertRepository, never()).save(any());
        verify(alertIncidentsChangedPublisher, never()).publish(any());
    }

    @Test
    void resolvesTheOpenAlertWhenTheReadingFallsBackUnderTheThreshold() {
        when(alertRepository.nextTransitionSequence()).thenReturn(42L);
        when(externalDeviceService.fetchSpaceIdByDeviceId(DEVICE_ID)).thenReturn(Optional.empty());
        when(externalThresholdService.fetchEnabledThresholdsByDeviceId(DEVICE_ID))
                .thenReturn(List.of(new ThresholdSummary("PM25", new BigDecimal("50.00"), true)));
        when(alertRepository.findFirstByDeviceIdAndMetricAndStatusIn(eq(DEVICE_ID), eq(MetricType.PM25), anyCollection()))
                .thenReturn(Optional.of(openAlert()));
        when(externalDeviceService.fetchHardwareIdByDeviceId(DEVICE_ID)).thenReturn(Optional.of("HW-0001"));
        when(alertRepository.save(any(Alert.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.handle(command(new BigDecimal("10.00")));

        ArgumentCaptor<Alert> saved = ArgumentCaptor.forClass(Alert.class);
        verify(alertRepository).save(saved.capture());
        assertThat(saved.getValue().getStatus()).isEqualTo(AlertStatus.RESOLVED);
        assertThat(saved.getValue().getResolvedAt()).isEqualTo(OCCURRED_AT);
        assertThat(saved.getValue().getTransitionSequence()).isEqualTo(42L);
    }

    @Test
    void acknowledgesAnActiveAlertForTheOwningHardware() {
        var alert = openAlert();
        when(alertRepository.nextTransitionSequence()).thenReturn(43L);
        when(alertRepository.findByIdForAcknowledgement(alert.getId())).thenReturn(Optional.of(alert));
        when(externalDeviceService.fetchHardwareIdByDeviceId(DEVICE_ID)).thenReturn(Optional.of("HW-0001"));

        var outcome = service.handle(new AcknowledgeEdgeAlertCommand(alert.getId(), "HW-0001", Instant.now()));

        assertThat(outcome).isEqualTo(AcknowledgementOutcome.OK);
        assertThat(alert.getStatus()).isEqualTo(AlertStatus.ACKNOWLEDGED);
        verify(alertRepository).save(alert);
        assertThat(alert.getTransitionSequence()).isEqualTo(43L);
    }

    /**
     * Ownership is checked before lifecycle state, so a wrong hardware id cannot tell an alert
     * apart from one that does not exist. It resolves through the device facade now; it used to
     * come from a {@code JOIN Device} in alerting's own repository.
     */
    @Test
    void hidesAnAlertBelongingToAnotherHardwareIdentity() {
        var alert = acknowledgedAlert();
        when(alertRepository.findByIdForAcknowledgement(alert.getId())).thenReturn(Optional.of(alert));
        when(externalDeviceService.fetchHardwareIdByDeviceId(DEVICE_ID)).thenReturn(Optional.of("HW-owner"));

        var outcome = service.handle(new AcknowledgeEdgeAlertCommand(alert.getId(), "HW-other", Instant.now()));

        assertThat(outcome).isEqualTo(AcknowledgementOutcome.NOT_FOUND);
        verify(alertRepository, never()).save(any());
    }

    @Test
    void reportsAConflictWhenTheAlertWasAlreadyAcknowledged() {
        var alert = acknowledgedAlert();
        when(alertRepository.findByIdForAcknowledgement(alert.getId())).thenReturn(Optional.of(alert));
        when(externalDeviceService.fetchHardwareIdByDeviceId(DEVICE_ID)).thenReturn(Optional.of("HW-0001"));

        var outcome = service.handle(new AcknowledgeEdgeAlertCommand(alert.getId(), "HW-0001", Instant.now()));

        assertThat(outcome).isEqualTo(AcknowledgementOutcome.CONFLICT);
        verify(alertRepository, never()).save(any());
    }

    @Test
    void reportsNotFoundWhenTheAlertDoesNotExist() {
        UUID alertId = UUID.randomUUID();
        when(alertRepository.findByIdForAcknowledgement(alertId)).thenReturn(Optional.empty());

        assertThat(service.handle(new AcknowledgeEdgeAlertCommand(alertId, "HW-0001", Instant.now())))
                .isEqualTo(AcknowledgementOutcome.NOT_FOUND);
    }

    @Test
    void aReceiptRecordsDeliveryWithoutTouchingTheBusinessStatus() {
        var alert = openAlert();
        alert.markTransition(10);
        when(alertRepository.findByIdForAcknowledgement(alert.getId())).thenReturn(Optional.of(alert));
        when(externalDeviceService.fetchHardwareIdByDeviceId(DEVICE_ID)).thenReturn(Optional.of("HW-0001"));
        var outcome = service.handle(new RecordEdgeAlertReceiptCommand(alert.getId(), "HW-0001", 10));
        assertThat(outcome).isEqualTo(AlertCommandService.ReceiptOutcome.OK);
        assertThat(alert.getStatus()).isEqualTo(AlertStatus.ACTIVE);
        assertThat(alert.getEdgeReceiptSequence()).isEqualTo(10L);
        verify(alertRepository).save(alert);
        verify(alertRepository, never()).nextTransitionSequence();
    }

    @Test
    void aReceiptFromAnotherUnitIsNotFoundAndRecordsNothing() {
        var alert = openAlert();
        when(alertRepository.findByIdForAcknowledgement(alert.getId())).thenReturn(Optional.of(alert));
        when(externalDeviceService.fetchHardwareIdByDeviceId(DEVICE_ID)).thenReturn(Optional.of("HW-owner"));
        assertThat(service.handle(new RecordEdgeAlertReceiptCommand(alert.getId(), "HW-other", 1)))
                .isEqualTo(AlertCommandService.ReceiptOutcome.NOT_FOUND);
        assertThat(alert.getEdgeReceiptSequence()).isNull();
        verify(alertRepository, never()).save(any());
    }

    private static EvaluateTelemetryForAlertsCommand command(BigDecimal pm25) {
        return new EvaluateTelemetryForAlertsCommand(DEVICE_ID, OCCURRED_AT, pm25,
                new BigDecimal("400"), new BigDecimal("22"), new BigDecimal("50"));
    }

    private static Alert openAlert() {
        return new Alert(DEVICE_ID, null, null, null, MetricType.PM25,
                new BigDecimal("50.00"), new BigDecimal("80.00"), "PM2.5 threshold exceeded",
                AlertSeverity.CRITICAL, OCCURRED_AT);
    }

    private static Alert acknowledgedAlert() {
        var alert = openAlert();
        alert.acknowledge();
        return alert;
    }
}
