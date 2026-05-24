package com.claircore.device.domain.model.commands;

import com.claircore.device.domain.model.valueobjects.MetricThreshold;
import com.claircore.device.domain.model.valueobjects.DeviceThresholdWriteIntent;
import com.claircore.device.domain.model.valueobjects.UserId;

import java.math.BigDecimal;
import java.util.UUID;

public record WriteDeviceThresholdCommand(
        UUID deviceId,
        UserId userId,
        MetricThreshold metric,
        BigDecimal value,
        boolean enabled,
        DeviceThresholdWriteIntent intent
) {
    public WriteDeviceThresholdCommand {
        if (deviceId == null) {
            throw new IllegalArgumentException("Device ID must not be null");
        }
        if (userId == null) {
            throw new IllegalArgumentException("User ID must not be null");
        }
        if (metric == null) {
            throw new IllegalArgumentException("Metric must not be null");
        }
        if (value == null) {
            throw new IllegalArgumentException("Value must not be null");
        }
        if (value.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Value must not be negative");
        }
        if (intent == null) {
            throw new IllegalArgumentException("Intent must not be null");
        }
    }
}
