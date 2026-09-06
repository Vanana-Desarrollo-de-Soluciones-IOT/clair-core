package com.claircore.analytics.infrastructure.config;

import com.claircore.analytics.domain.services.AqiCalculationDomainService;
import com.claircore.analytics.domain.services.AqiCalculationDomainServiceImpl;
import com.claircore.analytics.domain.services.MetricsAggregationDomainService;
import com.claircore.analytics.domain.services.MetricsAggregationDomainServiceImpl;
import com.claircore.analytics.domain.services.TrendAnalysisDomainService;
import com.claircore.analytics.domain.services.TrendAnalysisDomainServiceImpl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Registers the analytics domain services. They are plain objects with no framework annotations of
 * their own — the container has to be told about them here rather than finding them by scanning,
 * which is the point: the domain does not know it is running inside Spring.
 */
@Configuration
public class AnalyticsDomainServicesConfiguration {

    @Bean
    public AqiCalculationDomainService aqiCalculationDomainService() {
        return new AqiCalculationDomainServiceImpl();
    }

    @Bean
    public TrendAnalysisDomainService trendAnalysisDomainService() {
        return new TrendAnalysisDomainServiceImpl();
    }

    @Bean
    public MetricsAggregationDomainService metricsAggregationDomainService(
            AqiCalculationDomainService aqiCalculationDomainService) {
        return new MetricsAggregationDomainServiceImpl(aqiCalculationDomainService);
    }
}
