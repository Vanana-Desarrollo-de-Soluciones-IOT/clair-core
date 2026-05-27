package com.claircore.alerting.domain.model.events;

import com.claircore.alerting.domain.model.valueobjects.AlertSeverity;
import com.claircore.alerting.domain.model.valueobjects.MetricType;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class AlertCreatedEventTest {

    @Test
    void createsEventWithValidData() {
        var event = new AlertCreatedEvent(
                UUID.randomUUID(),
                UUID.randomUUID(),
                MetricType.CO2,
                AlertSeverity.WARNING,
                new BigDecimal("1000"),
                new BigDecimal("1200"),
                Instant.now()
        );
        assertNotNull(event);
        assertEquals(MetricType.CO2, event.metric());
    }

    @Test
    void throwsWhenAlertIdIsNull() {
        assertThrows(IllegalArgumentException.class, () ->
                new AlertCreatedEvent(null, UUID.randomUUID(), MetricType.CO2, AlertSeverity.LOW, BigDecimal.ONE, BigDecimal.TEN, Instant.now())
        );
    }

    @Test
    void throwsWhenOccurredAtIsNull() {
        assertThrows(IllegalArgumentException.class, () ->
                new AlertCreatedEvent(UUID.randomUUID(), UUID.randomUUID(), MetricType.CO2, AlertSeverity.LOW, BigDecimal.ONE, BigDecimal.TEN, null)
        );
    }
}
