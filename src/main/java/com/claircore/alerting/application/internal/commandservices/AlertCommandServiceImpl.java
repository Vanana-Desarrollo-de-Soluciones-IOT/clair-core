package com.claircore.alerting.application.internal.commandservices;

import com.claircore.alerting.application.commandservices.AlertCommandService;
import com.claircore.alerting.application.internal.outboundservices.acl.ExternalAlertingDeviceService;
import com.claircore.alerting.application.internal.outboundservices.acl.ExternalAlertingThresholdService;
import com.claircore.alerting.application.internal.outboundservices.acl.AlertIncidentsChangedPublisher;
import com.claircore.alerting.domain.model.aggregates.Alert;
import com.claircore.alerting.domain.model.commands.AcknowledgeEdgeAlertCommand;
import com.claircore.alerting.domain.model.commands.EvaluateTelemetryForAlertsCommand;
import com.claircore.alerting.domain.model.valueobjects.AlertSeverity;
import com.claircore.alerting.domain.model.valueobjects.AlertStatus;
import com.claircore.alerting.domain.model.valueobjects.MetricType;
import com.claircore.alerting.domain.repositories.AlertRepository;
import com.claircore.alerting.interfaces.events.AlertIncidentChangedIntegrationEvent;
import com.claircore.device.interfaces.acl.ThresholdSummary;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Service
public class AlertCommandServiceImpl implements AlertCommandService {

    private static final List<AlertStatus> OPEN_STATUSES = List.of(AlertStatus.ACTIVE, AlertStatus.ACKNOWLEDGED);

    private final AlertRepository alertRepository;
    private final ExternalAlertingThresholdService externalThresholdService;
    private final ExternalAlertingDeviceService externalDeviceService;
    private final AlertIncidentsChangedPublisher alertIncidentsChangedPublisher;

    public AlertCommandServiceImpl(
            AlertRepository alertRepository,
            ExternalAlertingThresholdService externalThresholdService,
            ExternalAlertingDeviceService externalDeviceService,
            AlertIncidentsChangedPublisher alertIncidentsChangedPublisher
    ) {
        this.alertRepository = alertRepository;
        this.externalThresholdService = externalThresholdService;
        this.externalDeviceService = externalDeviceService;
        this.alertIncidentsChangedPublisher = alertIncidentsChangedPublisher;
    }

    @Override
    @Transactional
    public void handle(EvaluateTelemetryForAlertsCommand command) {
        var spaceId = externalDeviceService.fetchSpaceIdByDeviceId(command.deviceId()).orElse(null);
        List<ThresholdSummary> enabled = externalThresholdService.fetchEnabledThresholdsByDeviceId(command.deviceId());

        Map<MetricType, BigDecimal> telemetry = telemetryValues(command);
        for (ThresholdSummary threshold : enabled) {
            MetricType metric = MetricType.valueOf(threshold.metric());
            BigDecimal actual = telemetry.get(metric);
            if (actual == null) continue;

            int comparison = actual.compareTo(threshold.value());
            if (comparison >= 0) {
                alertRepository.findFirstByDeviceIdAndMetricAndStatusIn(command.deviceId(), metric, OPEN_STATUSES)
                        .ifPresentOrElse(
                                existing -> {
                                    // Already active; avoid spamming duplicate alerts for every reading.
                                },
                                () -> {
                                    var spaceName = externalDeviceService.fetchSpaceNameBySpaceId(spaceId).orElse(null);
                                    var deviceName = externalDeviceService.fetchDeviceNameByDeviceId(command.deviceId()).orElse(null);
                                    var severity = calculateSeverity(actual, threshold.value());
                                    Alert created = alertRepository.save(new Alert(
                                            command.deviceId(),
                                            spaceId,
                                            spaceName,
                                            deviceName,
                                            metric,
                                            threshold.value(),
                                            actual,
                                            buildMessage(metric, threshold.value(), actual),
                                            severity,
                                            command.occurredAt()
                                    ));
                                    publishIncidentChanged(created);
                                }
                        );
            } else {
                alertRepository.findFirstByDeviceIdAndMetricAndStatusIn(command.deviceId(), metric, OPEN_STATUSES)
                        .ifPresent(openAlert -> {
                            openAlert.resolve(command.occurredAt());
                            Alert saved = alertRepository.save(openAlert);
                            publishIncidentChanged(saved);
                        });
            }
        }
    }

    /**
     * The ownership check comes first so an edge cannot learn the lifecycle state of an alert
     * belonging to another hardware identity. The hardware id is resolved through the device facade;
     * it used to come from a {@code JOIN Device} in alerting's own repository.
     */
    @Override
    @Transactional
    public AcknowledgementOutcome handle(AcknowledgeEdgeAlertCommand command) {
        return alertRepository.findByIdForAcknowledgement(command.alertId())
                .map(alert -> acknowledgeLoaded(alert, command))
                .orElse(AcknowledgementOutcome.NOT_FOUND);
    }

    private AcknowledgementOutcome acknowledgeLoaded(Alert alert, AcknowledgeEdgeAlertCommand command) {
        boolean ownedByCaller = externalDeviceService.fetchHardwareIdByDeviceId(alert.getDeviceId())
                .map(command.hardwareId()::equals)
                .orElse(false);
        if (!ownedByCaller) {
            return AcknowledgementOutcome.NOT_FOUND;
        }
        if (alert.getStatus() == AlertStatus.ACKNOWLEDGED || alert.getStatus() == AlertStatus.RESOLVED) {
            return AcknowledgementOutcome.CONFLICT;
        }
        if (alert.getStatus() != AlertStatus.ACTIVE) {
            return AcknowledgementOutcome.NOT_FOUND;
        }
        alert.acknowledge();
        alertRepository.save(alert);
        return AcknowledgementOutcome.OK;
    }

    private void publishIncidentChanged(Alert alert) {
        String hardwareId = externalDeviceService.fetchHardwareIdByDeviceId(alert.getDeviceId())
                .orElse(alert.getDeviceId().toString());

        alertIncidentsChangedPublisher.publish(new AlertIncidentChangedIntegrationEvent(
                alert.getId(),
                alert.getDeviceId(),
                hardwareId,
                alert.getSpaceId(),
                alert.getMetric(),
                alert.getThresholdValue(),
                alert.getActualValue(),
                alert.getMessage(),
                alert.getStatus(),
                alert.getOccurredAt(),
                alert.getResolvedAt()
        ));
    }

    private static Map<MetricType, BigDecimal> telemetryValues(EvaluateTelemetryForAlertsCommand command) {
        var map = new EnumMap<MetricType, BigDecimal>(MetricType.class);
        map.put(MetricType.PM25, command.pm25());
        map.put(MetricType.CO2, command.co2());
        map.put(MetricType.TEMPERATURE, command.temperature());
        map.put(MetricType.HUMIDITY, command.humidity());
        return map;
    }

    private static String buildMessage(MetricType metric, BigDecimal threshold, BigDecimal actual) {
        return "%s threshold exceeded: %s %s (threshold: %s %s)".formatted(
                metric.label(),
                actual.stripTrailingZeros().toPlainString(),
                metric.unit(),
                threshold.stripTrailingZeros().toPlainString(),
                metric.unit()
        );
    }

    private static AlertSeverity calculateSeverity(BigDecimal actual, BigDecimal threshold) {
        if (threshold.compareTo(BigDecimal.ZERO) == 0) {
            return AlertSeverity.LOW;
        }
        BigDecimal ratio = actual.divide(threshold, 4, java.math.RoundingMode.HALF_UP);
        if (ratio.compareTo(new BigDecimal("1.5")) >= 0) {
            return AlertSeverity.CRITICAL;
        }
        if (ratio.compareTo(new BigDecimal("1.2")) >= 0) {
            return AlertSeverity.WARNING;
        }
        return AlertSeverity.LOW;
    }
}
