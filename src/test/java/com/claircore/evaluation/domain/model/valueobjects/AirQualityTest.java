package com.claircore.evaluation.domain.model.valueobjects;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AirQualityTest {

    @Test
    void shouldCreateAirQualityWhenValuesAreValid() {
        // Arrange & Act
        AirQuality airQuality = new AirQuality(450.0, 23.5, 50.0);

        // Assert
        assertThat(airQuality.co2()).isEqualTo(450.0);
        assertThat(airQuality.temperature()).isEqualTo(23.5);
        assertThat(airQuality.humidity()).isEqualTo(50.0);
    }

    @Test
    void shouldThrowExceptionWhenCo2IsNull() {
        // Arrange, Act & Assert
        assertThatThrownBy(() -> new AirQuality(null, 23.5, 50.0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("co2 must not be null");
    }

    @Test
    void shouldThrowExceptionWhenTemperatureIsNull() {
        // Arrange, Act & Assert
        assertThatThrownBy(() -> new AirQuality(450.0, null, 50.0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("temperature must not be null");
    }

    @Test
    void shouldThrowExceptionWhenHumidityIsNull() {
        // Arrange, Act & Assert
        assertThatThrownBy(() -> new AirQuality(450.0, 23.5, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("humidity must not be null");
    }
}
