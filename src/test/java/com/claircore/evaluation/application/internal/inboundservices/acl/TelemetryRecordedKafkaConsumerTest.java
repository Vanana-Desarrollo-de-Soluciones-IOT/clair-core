package com.claircore.evaluation.application.internal.inboundservices.acl;

import com.claircore.evaluation.application.internal.outboundservices.acl.ExternalDeviceService;
import com.claircore.evaluation.domain.model.commands.EvaluateTelemetryCommand;
import com.claircore.evaluation.domain.services.TelemetryEvaluationCommandService;
import com.claircore.shared.infrastructure.kafka.KafkaInboxService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.Instant;
import java.time.LocalTime;
import java.util.Optional;
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TelemetryRecordedKafkaConsumerTest {

    @Mock
    private TelemetryEvaluationCommandService telemetryEvaluationCommandService;

    @Mock
    private ExternalDeviceService externalDeviceService;

    @Mock
    private KafkaInboxService kafkaInboxService;

    private ObjectMapper objectMapper;
    private TelemetryRecordedKafkaConsumer consumer;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        consumer = new TelemetryRecordedKafkaConsumer(
                telemetryEvaluationCommandService,
                objectMapper,
                externalDeviceService,
                kafkaInboxService
        );
    }

    @Test
    void shouldSkipProcessingWhenEventIsDuplicate() {
        // Arrange
        ConsumerRecord<String, String> record = new ConsumerRecord<>("topic", 0, 100L, "key", "payload");
        when(kafkaInboxService.shouldProcess("core-evaluation-consumer", "topic", 0, 100L)).thenReturn(false);

        // Act
        consumer.consume(record);

        // Assert
        verifyNoInteractions(telemetryEvaluationCommandService);
        verifyNoInteractions(externalDeviceService);
    }

    @Test
    void shouldThrowExceptionWhenPayloadIsInvalid() {
        // Arrange
        ConsumerRecord<String, String> record = new ConsumerRecord<>("topic", 0, 100L, "key", "{invalid json}");
        when(kafkaInboxService.shouldProcess("core-evaluation-consumer", "topic", 0, 100L)).thenReturn(true);

        // Act & Assert
        assertThatThrownBy(() -> consumer.consume(record))
                .isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(telemetryEvaluationCommandService);
    }

    @Test
    void shouldProcessAndStoreEvaluationWhenPayloadIsValidAndDeviceIsUuid() {
        // Arrange
        UUID deviceId = UUID.randomUUID();
        String json = "{"
                + "\"device_id\":\"" + deviceId + "\","
                + "\"device_time\":\"12:00:00\","
                + "\"uptime_seconds\":3600,"
                + "\"co2\":400.0,"
                + "\"temperature\":22.0,"
                + "\"humidity\":45.0,"
                + "\"pm1_0\":10,"
                + "\"pm2_5\":15,"
                + "\"pm10\":25,"
                + "\"wifi_status\":\"ONLINE\","
                + "\"network_name\":\"WiFi\","
                + "\"signal_strength\":-50,"
                + "\"country\":\"Chile\","
                + "\"health_status\":85,"
                + "\"status\":\"STABLE\","
                + "\"recorded_at\":\"" + Instant.now().toString() + "\""
                + "}";
        ConsumerRecord<String, String> record = new ConsumerRecord<>("topic", 0, 100L, "key", json);
        when(kafkaInboxService.shouldProcess("core-evaluation-consumer", "topic", 0, 100L)).thenReturn(true);

        // Act
        consumer.consume(record);

        // Assert
        verify(telemetryEvaluationCommandService).handle(any(EvaluateTelemetryCommand.class));
        verify(kafkaInboxService).markProcessed("core-evaluation-consumer", "topic", 0, 100L);
    }

    @Test
    void shouldSkipEventGracefullyWhenDeviceIsUnknown() {
        // Arrange
        String unknownHardwareId = "HW-UNKNOWN";
        String json = "{"
                + "\"device_id\":\"" + unknownHardwareId + "\","
                + "\"device_time\":\"12:00:00\","
                + "\"uptime_seconds\":3600,"
                + "\"co2\":400.0,"
                + "\"temperature\":22.0,"
                + "\"humidity\":45.0,"
                + "\"pm1_0\":10,"
                + "\"pm2_5\":15,"
                + "\"pm10\":25,"
                + "\"wifi_status\":\"ONLINE\","
                + "\"network_name\":\"WiFi\","
                + "\"signal_strength\":-50,"
                + "\"country\":\"Chile\","
                + "\"health_status\":85,"
                + "\"status\":\"STABLE\","
                + "\"recorded_at\":\"" + Instant.now().toString() + "\""
                + "}";
        ConsumerRecord<String, String> record = new ConsumerRecord<>("topic", 0, 100L, "key", json);
        when(kafkaInboxService.shouldProcess("core-evaluation-consumer", "topic", 0, 100L)).thenReturn(true);
        when(externalDeviceService.findDeviceIdByHardwareId(unknownHardwareId)).thenReturn(Optional.empty());

        // Act
        consumer.consume(record);

        // Assert
        verifyNoInteractions(telemetryEvaluationCommandService);
        verify(kafkaInboxService).markProcessed("core-evaluation-consumer", "topic", 0, 100L);
    }

    @Test
    void shouldThrowExceptionWhenDeviceTimeIsInvalid() {
        // Arrange
        UUID deviceId = UUID.randomUUID();
        String json = "{"
                + "\"device_id\":\"" + deviceId + "\","
                + "\"device_time\":\"invalid-time-format\","
                + "\"uptime_seconds\":3600,"
                + "\"co2\":400.0,"
                + "\"temperature\":22.0,"
                + "\"humidity\":45.0,"
                + "\"pm1_0\":10,"
                + "\"pm2_5\":15,"
                + "\"pm10\":25,"
                + "\"wifi_status\":\"ONLINE\","
                + "\"network_name\":\"WiFi\","
                + "\"signal_strength\":-50,"
                + "\"country\":\"Chile\","
                + "\"health_status\":85,"
                + "\"status\":\"STABLE\","
                + "\"recorded_at\":\"" + Instant.now().toString() + "\""
                + "}";
        ConsumerRecord<String, String> record = new ConsumerRecord<>("topic", 0, 100L, "key", json);
        when(kafkaInboxService.shouldProcess("core-evaluation-consumer", "topic", 0, 100L)).thenReturn(true);

        // Act & Assert
        assertThatThrownBy(() -> consumer.consume(record))
                .isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(telemetryEvaluationCommandService);
    }

    @Test
    void shouldCacheDeviceIdResolutionAndAvoidRedundantOutboundCalls() {
        // Arrange
        String hardwareId = "HW-777";
        UUID resolvedUuid = UUID.randomUUID();
        String json = "{"
                + "\"device_id\":\"" + hardwareId + "\","
                + "\"device_time\":\"12:00:00\","
                + "\"uptime_seconds\":3600,"
                + "\"co2\":400.0,"
                + "\"temperature\":22.0,"
                + "\"humidity\":45.0,"
                + "\"pm1_0\":10,"
                + "\"pm2_5\":15,"
                + "\"pm10\":25,"
                + "\"wifi_status\":\"ONLINE\","
                + "\"network_name\":\"WiFi\","
                + "\"signal_strength\":-50,"
                + "\"country\":\"Chile\","
                + "\"health_status\":85,"
                + "\"status\":\"STABLE\","
                + "\"recorded_at\":\"" + Instant.now().toString() + "\""
                + "}";
        
        ConsumerRecord<String, String> record1 = new ConsumerRecord<>("topic", 0, 100L, "key1", json);
        ConsumerRecord<String, String> record2 = new ConsumerRecord<>("topic", 0, 101L, "key2", json);

        when(kafkaInboxService.shouldProcess(any(), any(), anyInt(), eq(100L))).thenReturn(true);
        when(kafkaInboxService.shouldProcess(any(), any(), anyInt(), eq(101L))).thenReturn(true);
        when(externalDeviceService.findDeviceIdByHardwareId(hardwareId)).thenReturn(Optional.of(resolvedUuid));

        // Act - Process the first record
        consumer.consume(record1);
        
        // Act - Process the second record (same hardware_id)
        consumer.consume(record2);

        // Assert - externalDeviceService should be called exactly once
        verify(externalDeviceService, times(1)).findDeviceIdByHardwareId(hardwareId);
        verify(telemetryEvaluationCommandService, times(2)).handle(any(EvaluateTelemetryCommand.class));
    }
}
