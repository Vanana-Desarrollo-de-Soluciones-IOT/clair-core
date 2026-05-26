package com.claircore.analytics.domain.services;

import com.claircore.analytics.domain.model.queries.GetDashboardMetricsQuery;
import com.claircore.analytics.domain.model.valueobjects.KpiDashboardMetrics;

import java.util.Optional;

public interface KpiDashboardMetricsQueryService {

    Optional<KpiDashboardMetrics> handle(GetDashboardMetricsQuery query);
}
