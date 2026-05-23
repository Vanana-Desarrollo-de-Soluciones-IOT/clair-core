package com.claircore.evaluation.application.internal.inboundservices.acl;

import com.claircore.evaluation.domain.model.commands.EvaluateTelemetryCommand;
import com.claircore.evaluation.domain.model.valueobjects.*;
import com.claircore.evaluation.domain.services.TelemetryEvaluationCommandService;
import com.claircore.evaluation.infrastructure.kafka.EvaluationKafkaTopics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.time.Instant;

/**
 * Kafka consumer that processes TelemetryRecorded integration events
 * from the Edge and stores them as telemetry evaluations.
 */
@Service
public class TelemetryRecordedKafkaConsumer {

    private static final Logger LOGGER = LoggerFactory.getLogger(TelemetryRecordedKafkaConsumer.class);

    private final TelemetryEvaluationCommandService telemetryEvaluationCommandService;

    public TelemetryRecordedKafkaConsumer(TelemetryEvaluationCommandService telemetryEvaluationCommandService) {
        this.telemetryEvaluationCommandService = telemetryEvaluationCommandService;
    }

    @KafkaListener(
            topics = "clair.device.telemetry.recorded",
            groupId = "core-evaluation-consumer",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consume(TelemetryRecordedIntegrationEvent event) {
        LOGGER.info("Consuming telemetry record for device {}", event.deviceId());

        try {
            var command = new EvaluateTelemetryCommand(
                    new com.claircore.evaluation.domain.model.valueobjects.DeviceId(java.util.UUID.fromString(event.deviceId())),
                    event.deviceTime(),
                    String.valueOf(event.uptimeSeconds()),
                    new AirQuality(event.co2(), event.temperature(), event.humidity()),
                    new ParticulateMatter(event.pm1_0(), event.pm2_5(), event.pm10()),
                    new Connectivity(event.wifiStatus(), event.networkName(), event.signalStrength()),
                    new Location(event.country()),
                    event.healthStatus(),
                    event.status(),
                    Instant.parse(event.recordedAt())
            );

            telemetryEvaluationCommandService.handle(command);
        } catch (Exception e) {
            LOGGER.error("Failed to process telemetry event for device {}", event.deviceId(), e);
        }
    }
}
