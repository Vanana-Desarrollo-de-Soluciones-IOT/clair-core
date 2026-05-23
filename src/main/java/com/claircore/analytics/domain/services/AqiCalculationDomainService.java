package com.claircore.analytics.domain.services;

import com.claircore.analytics.domain.model.valueobjects.AirQualityIndex;

public interface AqiCalculationDomainService {

    AirQualityIndex calculateAqi(Double pm2_5, Double co2);
}
