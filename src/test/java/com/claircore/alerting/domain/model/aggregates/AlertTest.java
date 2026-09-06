package com.claircore.alerting.domain.model.aggregates;

import com.claircore.alerting.domain.model.valueobjects.AlertSeverity;
import com.claircore.alerting.domain.model.valueobjects.AlertStatus;
import com.claircore.alerting.domain.model.valueobjects.MetricType;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class AlertTest {

    @Test
    void createsAlertWithActiveStatus() {
        var alert = newAlert();
        assertEquals(AlertStatus.ACTIVE, alert.getStatus());
    }

    @Test
    void createsAlertWithProvidedSeverity() {
        var alert = newAlert();
        assertEquals(AlertSeverity.CRITICAL, alert.getSeverity());
    }

    @Test
    void resolvesAlertAndSetsResolvedAt() {
        var alert = newAlert();
        var now = Instant.now();
        alert.resolve(now);
        assertEquals(AlertStatus.RESOLVED, alert.getStatus());
        assertEquals(now, alert.getResolvedAt());
    }

    @Test
    void resolvesAlertThrowsWhenNullInstant() {
        var alert = newAlert();
        assertThrows(IllegalArgumentException.class, () -> alert.resolve(null));
    }

    @Test
    void createsAlertWithSpaceAndDeviceNames() {
        var alert = newAlert();
        assertEquals("Test Space", alert.getSpaceName());
        assertEquals("Test Device", alert.getDeviceName());
    }

    private static Alert newAlert() {
        return new Alert(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "Test Space",
                "Test Device",
                MetricType.PM25,
                new BigDecimal("50.00"),
                new BigDecimal("80.00"),
                "PM2.5 threshold exceeded",
                AlertSeverity.CRITICAL,
                Instant.now()
        );
    }
}
