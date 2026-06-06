package com.claircore.analytics.domain.model.valueobjects;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AirQualityIndexTest {

    @Test
    void shouldCreateAirQualityIndexWhenValueIsValid() {
        var aqi = new AirQualityIndex(100, AqiCategory.MODERATE);
        assertNotNull(aqi);
        assertEquals(100, aqi.value());
        assertEquals(AqiCategory.MODERATE, aqi.category());
    }

    @Test
    void shouldThrowExceptionWhenValueIsNull() {
        assertThrows(IllegalArgumentException.class, () -> 
            new AirQualityIndex(null, AqiCategory.GOOD)
        );
    }

    @Test
    void shouldThrowExceptionWhenValueIsNegative() {
        assertThrows(IllegalArgumentException.class, () -> 
            new AirQualityIndex(-1, AqiCategory.GOOD)
        );
    }

    @Test
    void shouldThrowExceptionWhenValueIsGreaterThan500() {
        assertThrows(IllegalArgumentException.class, () -> 
            new AirQualityIndex(501, AqiCategory.HAZARDOUS)
        );
    }

    @Test
    void shouldThrowExceptionWhenCategoryIsNull() {
        assertThrows(IllegalArgumentException.class, () -> 
            new AirQualityIndex(50, null)
        );
    }
}
