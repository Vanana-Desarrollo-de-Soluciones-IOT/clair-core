package com.claircore.alerting.application.internal.commandservices;

import com.claircore.alerting.application.internal.outboundservices.acl.ExternalAlertingDeviceService;
import com.claircore.alerting.application.internal.outboundservices.acl.ExternalAlertingThresholdService;
import com.claircore.alerting.application.internal.outboundservices.acl.AlertIncidentChangedIntegrationEvent;
import com.claircore.alerting.application.internal.outboundservices.acl.AlertIncidentsChangedKafkaPublisher;
import com.claircore.alerting.domain.model.commands.EvaluateTelemetryForAlertsCommand;
import com.claircore.alerting.domain.model.entities.Alert;
import com.claircore.alerting.domain.model.valueobjects.AlertStatus;
import com.claircore.alerting.domain.model.valueobjects.MetricType;
import com.claircore.alerting.domain.services.AlertCommandService;
import com.claircore.alerting.infrastructure.persistence.jpa.repositories.AlertRepository;
import com.claircore.device.domain.model.valueobjects.DeviceMetricThresholdConfiguration;
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
    private final AlertIncidentsChangedKafkaPublisher alertIncidentsChangedKafkaPublisher;

    public AlertCommandServiceImpl(
            AlertRepository alertRepository,
            ExternalAlertingThresholdService externalThresholdService,
            ExternalAlertingDeviceService externalDeviceService,
            AlertIncidentsChangedKafkaPublisher alertIncidentsChangedKafkaPublisher
    ) {
        this.alertRepository = alertRepository;
        this.externalThresholdService = externalThresholdService;
        this.externalDeviceService = externalDeviceService;
        this.alertIncidentsChangedKafkaPublisher = alertIncidentsChangedKafkaPublisher;
    }

    @Override
    @Transactional
    public void handle(EvaluateTelemetryForAlertsCommand command) {
        var spaceId = externalDeviceService.fetchSpaceIdByDeviceId(command.deviceId()).orElse(null);
        List<DeviceMetricThresholdConfiguration> enabled = externalThresholdService.fetchEnabledThresholdsByDeviceId(command.deviceId());

        Map<MetricType, BigDecimal> telemetry = telemetryValues(command);
        for (DeviceMetricThresholdConfiguration threshold : enabled) {
            MetricType metric = MetricType.valueOf(threshold.metric().name());
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
                                    Alert created = alertRepository.save(new Alert(
                                            command.deviceId(),
                                            spaceId,
                                            metric,
                                            threshold.value(),
                                            actual,
                                            buildMessage(metric, threshold.value(), actual),
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

    private void publishIncidentChanged(Alert alert) {
        String hardwareId = externalDeviceService.fetchHardwareIdByDeviceId(alert.getDeviceId())
                .orElse(alert.getDeviceId().toString());

        alertIncidentsChangedKafkaPublisher.publish(new AlertIncidentChangedIntegrationEvent(
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
}
