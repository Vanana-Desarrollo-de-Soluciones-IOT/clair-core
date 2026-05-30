package com.claircore.analytics.domain.services;

import com.claircore.analytics.domain.model.valueobjects.AggregatedMetrics;
import com.claircore.analytics.domain.model.valueobjects.DeviceMetricsSnapshot;
import java.util.List;

public interface MetricsAggregationDomainService {
    AggregatedMetrics aggregate(List<DeviceMetricsSnapshot> snapshots);
}
