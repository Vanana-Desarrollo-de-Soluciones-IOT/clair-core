package com.claircore.analytics.domain.services;

import com.claircore.analytics.domain.model.valueobjects.AqiCategory;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AqiCalculationDomainServiceImplTest {

    private final AqiCalculationDomainServiceImpl calculator = new AqiCalculationDomainServiceImpl();

    @Test
    void shouldCalculateCorrectAqiAndCategoryForPm25Ranges() {
        // PM2.5 = 6.0 (Midpoint of 0.0 - 12.0) -> AQI around 25 -> GOOD
        var aqi1 = calculator.calculateAqi(6.0, 300.0);
        assertEquals(AqiCategory.GOOD, aqi1.category());
        assertTrue(aqi1.value() >= 0 && aqi1.value() <= 50);

        // PM2.5 = 20.0 (Inside 12.1 - 35.4) -> MODERATE
        var aqi2 = calculator.calculateAqi(20.0, 300.0);
        assertEquals(AqiCategory.MODERATE, aqi2.category());

        // PM2.5 = 45.0 (Inside 35.5 - 55.4) -> UNHEALTHY_FOR_SENSITIVE
        var aqi3 = calculator.calculateAqi(45.0, 300.0);
        assertEquals(AqiCategory.UNHEALTHY_FOR_SENSITIVE, aqi3.category());

        // PM2.5 = 100.0 (Inside 55.5 - 150.4) -> UNHEALTHY
        var aqi4 = calculator.calculateAqi(100.0, 300.0);
        assertEquals(AqiCategory.UNHEALTHY, aqi4.category());

        // PM2.5 = 200.0 (Inside 150.5 - 250.4) -> VERY_UNHEALTHY
        var aqi5 = calculator.calculateAqi(200.0, 300.0);
        assertEquals(AqiCategory.VERY_UNHEALTHY, aqi5.category());

        // PM2.5 = 350.0 (Inside 250.5 - 500.4) -> HAZARDOUS
        var aqi6 = calculator.calculateAqi(350.0, 300.0);
        assertEquals(AqiCategory.HAZARDOUS, aqi6.category());
    }

    @Test
    void shouldCalculateCorrectAqiAndCategoryForCo2Ranges() {
        // CO2 = 200.0 -> GOOD (AQI <= 50)
        var aqi1 = calculator.calculateAqi(0.0, 200.0);
        assertEquals(AqiCategory.GOOD, aqi1.category());

        // CO2 = 700 -> MODERATE (AQI 51 - 100)
        var aqi2 = calculator.calculateAqi(0.0, 700.0);
        assertEquals(AqiCategory.MODERATE, aqi2.category());

        // CO2 = 1200 -> UNHEALTHY_FOR_SENSITIVE (AQI 101 - 150)
        var aqi3 = calculator.calculateAqi(0.0, 1200.0);
        assertEquals(AqiCategory.UNHEALTHY_FOR_SENSITIVE, aqi3.category());
    }

    @Test
    void shouldSelectMaxSubIndexAsFinalAqi() {
        // PM2.5 subindex is MODERATE (AQI ~ 70), CO2 subindex is UNHEALTHY (AQI ~ 170) -> Final AQI should be UNHEALTHY
        var aqi = calculator.calculateAqi(20.0, 1800.0);
        assertEquals(AqiCategory.UNHEALTHY, aqi.category());
    }

    @Test
    void shouldReturnDefaultAqiForNullOrNegativeConcentration() {
        var aqi1 = calculator.calculateAqi(null, null);
        assertEquals(0, aqi1.value());
        assertEquals(AqiCategory.GOOD, aqi1.category());

        var aqi2 = calculator.calculateAqi(-10.0, -5.0);
        assertEquals(0, aqi2.value());
        assertEquals(AqiCategory.GOOD, aqi2.category());
    }
}
