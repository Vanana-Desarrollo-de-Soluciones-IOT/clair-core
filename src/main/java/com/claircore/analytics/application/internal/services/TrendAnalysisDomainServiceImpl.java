package com.claircore.analytics.application.internal.services;

import com.claircore.analytics.domain.model.valueobjects.MetricTrend;
import com.claircore.analytics.domain.services.TrendAnalysisDomainService;
import org.springframework.stereotype.Service;

@Service
public class TrendAnalysisDomainServiceImpl implements TrendAnalysisDomainService {

    @Override
    public MetricTrend calculateTrend(Double currentValue, Double previousValue) {
        if (previousValue == null) {
            return new MetricTrend(currentValue, null, null);
        }
        if (previousValue == 0.0) {
            return new MetricTrend(currentValue, 0.0, null);
        }
        double delta = ((currentValue - previousValue) / Math.abs(previousValue)) * 100.0;
        double rounded = Math.round(delta * 100.0) / 100.0;
        return new MetricTrend(currentValue, previousValue, rounded);
    }
}
