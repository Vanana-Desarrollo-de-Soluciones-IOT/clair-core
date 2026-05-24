package com.claircore.device.domain.model.queries;

import com.claircore.device.domain.model.valueobjects.MetricThreshold;

import java.util.UUID;

public record GetDeviceThresholdByMetricQuery(
        UUID assignmentId,
        MetricThreshold metric
) {
    public GetDeviceThresholdByMetricQuery {
        if (assignmentId == null) {
            throw new IllegalArgumentException("Assignment ID must not be null");
        }
        if (metric == null) {
            throw new IllegalArgumentException("Metric must not be null");
        }
    }
}