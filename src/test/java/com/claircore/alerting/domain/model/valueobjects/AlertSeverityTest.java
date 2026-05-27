package com.claircore.alerting.domain.model.valueobjects;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class AlertSeverityTest {

    @Test
    void criticalWhenRatioAboveOnePointFive() {
        assertEquals(AlertSeverity.CRITICAL, calculate(new BigDecimal("150"), new BigDecimal("100")));
    }

    @Test
    void warningWhenRatioAboveOnePointTwo() {
        assertEquals(AlertSeverity.WARNING, calculate(new BigDecimal("125"), new BigDecimal("100")));
    }

    @Test
    void lowWhenRatioAboveOne() {
        assertEquals(AlertSeverity.LOW, calculate(new BigDecimal("105"), new BigDecimal("100")));
    }

    @Test
    void lowWhenThresholdIsZero() {
        assertEquals(AlertSeverity.LOW, calculate(new BigDecimal("50"), BigDecimal.ZERO));
    }

    @Test
    void criticalWhenExactlyOnePointFive() {
        assertEquals(AlertSeverity.CRITICAL, calculate(new BigDecimal("150"), new BigDecimal("100")));
    }

    private static AlertSeverity calculate(BigDecimal actual, BigDecimal threshold) {
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
