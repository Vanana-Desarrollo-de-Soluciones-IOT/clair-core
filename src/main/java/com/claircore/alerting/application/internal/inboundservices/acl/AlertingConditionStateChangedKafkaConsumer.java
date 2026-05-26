package com.claircore.alerting.application.internal.inboundservices.acl;

import com.claircore.alerting.application.internal.outboundservices.acl.ExternalAlertingDeviceService;
import com.claircore.alerting.domain.model.commands.RecordAlertConditionStateChangedCommand;
import com.claircore.alerting.domain.model.valueobjects.AlertConditionState;
import com.claircore.alerting.domain.model.valueobjects.MetricType;
import com.claircore.alerting.domain.services.AlertCommandService;
import com.claircore.shared.infrastructure.kafka.KafkaInboxService;
import com.fasterxml.jackson.core.json.JsonReadFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
public class AlertingConditionStateChangedKafkaConsumer {

    // Edge/Embedded publishes condition transitions (NORMAL/CRITICAL) for a metric.
    private static final String TOPIC = "clair.device.alert.condition.changed";
    private static final String CONSUMER_GROUP_ID = "core-alerting-condition-consumer";

    private static final Logger LOGGER = LoggerFactory.getLogger(AlertingConditionStateChangedKafkaConsumer.class);

    private final AlertCommandService alertCommandService;
    private final ObjectMapper objectMapper;
    private final ExternalAlertingDeviceService externalDeviceService;
    private final KafkaInboxService kafkaInboxService;

    public AlertingConditionStateChangedKafkaConsumer(
            AlertCommandService alertCommandService,
            ObjectMapper objectMapper,
            ExternalAlertingDeviceService externalDeviceService,
            KafkaInboxService kafkaInboxService
    ) {
        this.alertCommandService = alertCommandService;
        // Edge currently publishes snake_case keys.
        // Be lenient with non-strict JSON numbers (some emitters send leading zeros, e.g. 04.9).
        this.objectMapper = objectMapper.copy()
                .setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE)
                .enable(JsonReadFeature.ALLOW_LEADING_ZEROS_FOR_NUMBERS.mappedFeature());
        this.externalDeviceService = externalDeviceService;
        this.kafkaInboxService = kafkaInboxService;
    }

    @KafkaListener(
            topics = TOPIC,
            groupId = CONSUMER_GROUP_ID,
            containerFactory = "kafkaListenerContainerFactory"
    )
    @Transactional
    public void consume(ConsumerRecord<String, String> record) {
        if (!kafkaInboxService.shouldProcess(CONSUMER_GROUP_ID, record.topic(), record.partition(), record.offset())) {
            return;
        }

        String payload = record.value();
        AlertConditionStateChangedIntegrationEvent event;
        try {
            event = objectMapper.readValue(payload, AlertConditionStateChangedIntegrationEvent.class);
        } catch (Exception e) {
            LOGGER.error("Failed to deserialize alert condition event payload: {}", payload, e);
            throw new IllegalArgumentException("Invalid alert condition event payload", e);
        }

        try {
            UUID resolvedDeviceId = resolveDeviceId(event.deviceId(), event.hardwareId()).orElse(null);
            if (resolvedDeviceId == null) {
                LOGGER.warn("Skipping alert condition event: unknown device identifier {}", event.deviceId());
                kafkaInboxService.markProcessed(CONSUMER_GROUP_ID, record.topic(), record.partition(), record.offset());
                return;
            }

            MetricType metric;
            try {
                metric = MetricType.valueOf(event.metric());
            } catch (Exception e) {
                LOGGER.warn("Skipping alert condition event: unknown metric '{}'", event.metric());
                kafkaInboxService.markProcessed(CONSUMER_GROUP_ID, record.topic(), record.partition(), record.offset());
                return;
            }

            AlertConditionState state;
            try {
                state = AlertConditionState.valueOf(event.conditionState());
            } catch (Exception e) {
                LOGGER.warn("Skipping alert condition event: unknown condition_state '{}'", event.conditionState());
                kafkaInboxService.markProcessed(CONSUMER_GROUP_ID, record.topic(), record.partition(), record.offset());
                return;
            }

            Instant occurredAt = parseOccurredAt(event);
            alertCommandService.handle(new RecordAlertConditionStateChangedCommand(resolvedDeviceId, metric, state, occurredAt));
            kafkaInboxService.markProcessed(CONSUMER_GROUP_ID, record.topic(), record.partition(), record.offset());
        } catch (Exception e) {
            LOGGER.error("Failed to process alert condition event, device {}", event.deviceId(), e);
            throw e;
        }
    }

    private Optional<UUID> resolveDeviceId(String deviceIdOrHardwareId, String hardwareId) {
        var fromDeviceField = resolveDeviceIdString(deviceIdOrHardwareId);
        if (fromDeviceField.isPresent()) return fromDeviceField;

        if (hardwareId == null || hardwareId.isBlank()) return Optional.empty();
        return externalDeviceService.fetchDeviceIdByHardwareId(hardwareId);
    }

    private Optional<UUID> resolveDeviceIdString(String deviceIdOrHardwareId) {
        if (deviceIdOrHardwareId == null || deviceIdOrHardwareId.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(UUID.fromString(deviceIdOrHardwareId));
        } catch (IllegalArgumentException ignored) {
            return externalDeviceService.fetchDeviceIdByHardwareId(deviceIdOrHardwareId);
        }
    }

    private static Instant parseOccurredAt(AlertConditionStateChangedIntegrationEvent event) {
        String candidate = (event.occurredAt() != null && !event.occurredAt().isBlank())
                ? event.occurredAt()
                : event.recordedAt();
        return Instant.parse(candidate);
    }
}
