package com.claircore.evaluation.domain.model.valueobjects;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LocationTest {

    @Test
    void shouldCreateLocationWhenValueIsValid() {
        // Arrange & Act
        Location location = new Location("Peru");

        // Assert
        assertThat(location.country()).isEqualTo("Peru");
    }

    @Test
    void shouldTrimCountryValueOnCreation() {
        // Arrange & Act
        Location location = new Location("  Colombia  ");

        // Assert
        assertThat(location.country()).isEqualTo("Colombia");
    }

    @Test
    void shouldThrowExceptionWhenCountryIsNull() {
        // Arrange, Act & Assert
        assertThatThrownBy(() -> new Location(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("country must not be null or blank");
    }

    @Test
    void shouldThrowExceptionWhenCountryIsBlank() {
        // Arrange, Act & Assert
        assertThatThrownBy(() -> new Location("   "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("country must not be null or blank");
    }
}
