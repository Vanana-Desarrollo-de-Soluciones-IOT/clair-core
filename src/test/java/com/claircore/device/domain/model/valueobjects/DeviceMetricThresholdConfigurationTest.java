package com.claircore.device.domain.model.valueobjects;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrowsExactly;

class DeviceMetricThresholdConfigurationTest {

    @Test
    void shouldCreateThresholdConfigurationWhenValuesAreValid() {
        DeviceMetricThresholdConfiguration configuration = new DeviceMetricThresholdConfiguration(
                MetricThreshold.PM25,
                new BigDecimal("35.5"),
                true
        );

        assertEquals(MetricThreshold.PM25, configuration.metric());
        assertEquals(new BigDecimal("35.5"), configuration.value());
        assertEquals(true, configuration.enabled());
    }

    @Test
    void shouldThrowExceptionWhenMetricIsNull() {
        IllegalArgumentException exception = assertThrowsExactly(
                IllegalArgumentException.class,
                () -> new DeviceMetricThresholdConfiguration(null, new BigDecimal("1.0"), true)
        );

        assertEquals("Metric must not be null", exception.getMessage());
    }

    @Test
    void shouldThrowExceptionWhenValueIsNull() {
        IllegalArgumentException exception = assertThrowsExactly(
                IllegalArgumentException.class,
                () -> new DeviceMetricThresholdConfiguration(MetricThreshold.PM25, null, true)
        );

        assertEquals("Value must not be null", exception.getMessage());
    }

    @Test
    void shouldThrowExceptionWhenValueIsNegative() {
        IllegalArgumentException exception = assertThrowsExactly(
                IllegalArgumentException.class,
                () -> new DeviceMetricThresholdConfiguration(MetricThreshold.PM25, new BigDecimal("-1"), true)
        );

        assertEquals("Value must not be negative", exception.getMessage());
    }
}
