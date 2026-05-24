package com.claircore.device.domain.model.commands;

import com.claircore.device.domain.model.valueobjects.MetricThreshold;
import com.claircore.device.domain.model.valueobjects.UserId;

import java.util.UUID;

public record RemoveDeviceThresholdCommand(
        UUID deviceId,
        UserId userId,
        MetricThreshold metric
) {
    public RemoveDeviceThresholdCommand {
        if (deviceId == null) {
            throw new IllegalArgumentException("Device ID must not be null");
        }
        if (userId == null) {
            throw new IllegalArgumentException("User ID must not be null");
        }
        if (metric == null) {
            throw new IllegalArgumentException("Metric must not be null");
        }
    }
}