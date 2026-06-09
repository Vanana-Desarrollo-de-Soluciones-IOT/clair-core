package com.claircore.analytics.domain.services;

import com.claircore.analytics.domain.model.valueobjects.MetricTrend;

public interface TrendAnalysisDomainService {

    MetricTrend calculateTrend(Double currentValue, Double previousValue);
}
