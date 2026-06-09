package com.claircore.analytics.domain.services;

import com.claircore.analytics.domain.model.valueobjects.AirQualityIndex;
import com.claircore.analytics.domain.model.valueobjects.AqiCategory;
import com.claircore.analytics.domain.services.AqiCalculationDomainService;
import org.springframework.stereotype.Service;

@Service
public class AqiCalculationDomainServiceImpl implements AqiCalculationDomainService {

    @Override
    public AirQualityIndex calculateAqi(Double pm2_5, Double co2) {
        int pm25Aqi = subIndexPm25(pm2_5);
        int co2Aqi = subIndexCo2(co2);
        int finalAqi = Math.max(pm25Aqi, co2Aqi);
        AqiCategory category = resolveCategory(finalAqi);
        return new AirQualityIndex(finalAqi, category);
    }

    private int subIndexPm25(Double concentration) {
        return piecewiseLinear(concentration,
                0.0, 12.0, 0, 50,
                12.1, 35.4, 51, 100,
                35.5, 55.4, 101, 150,
                55.5, 150.4, 151, 200,
                150.5, 250.4, 201, 300,
                250.5, 500.4, 301, 400,
                500.5, 999.9, 401, 500);
    }

    private int subIndexCo2(Double concentration) {
        return piecewiseLinear(concentration,
                0.0, 400.0, 0, 50,
                401.0, 1000.0, 51, 100,
                1001.0, 1500.0, 101, 150,
                1501.0, 2500.0, 151, 200,
                2501.0, 5000.0, 201, 300,
                5001.0, 99999.0, 301, 500);
    }

    private int piecewiseLinear(Double c, double... breakpoints) {
        if (c == null || c < 0) {
            return 0;
        }
        for (int i = 0; i < breakpoints.length; i += 4) {
            double bpLo = breakpoints[i];
            double bpHi = breakpoints[i + 1];
            int iLo = (int) breakpoints[i + 2];
            int iHi = (int) breakpoints[i + 3];
            if (c >= bpLo && c <= bpHi) {
                return (int) Math.round(
                        ((double) (iHi - iLo) / (bpHi - bpLo)) * (c - bpLo) + iLo
                );
            }
        }
        return 500;
    }

    private AqiCategory resolveCategory(int aqi) {
        if (aqi <= 50) return AqiCategory.GOOD;
        if (aqi <= 100) return AqiCategory.MODERATE;
        if (aqi <= 150) return AqiCategory.UNHEALTHY_FOR_SENSITIVE;
        if (aqi <= 200) return AqiCategory.UNHEALTHY;
        if (aqi <= 300) return AqiCategory.VERY_UNHEALTHY;
        return AqiCategory.HAZARDOUS;
    }
}
