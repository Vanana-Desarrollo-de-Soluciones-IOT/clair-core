package com.claircore.evaluation.domain.model.valueobjects;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ParticulateMatterTest {

    @Test
    void shouldCreateParticulateMatterWhenValuesAreValid() {
        // Arrange & Act
        ParticulateMatter particulateMatter = new ParticulateMatter(12, 18, 35);

        // Assert
        assertThat(particulateMatter.pm1_0()).isEqualTo(12);
        assertThat(particulateMatter.pm2_5()).isEqualTo(18);
        assertThat(particulateMatter.pm10()).isEqualTo(35);
    }

    @Test
    void shouldThrowExceptionWhenPm1_0IsNull() {
        // Arrange, Act & Assert
        assertThatThrownBy(() -> new ParticulateMatter(null, 18, 35))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("pm1_0 must not be null");
    }

    @Test
    void shouldThrowExceptionWhenPm2_5IsNull() {
        // Arrange, Act & Assert
        assertThatThrownBy(() -> new ParticulateMatter(12, null, 35))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("pm2_5 must not be null");
    }

    @Test
    void shouldThrowExceptionWhenPm10IsNull() {
        // Arrange, Act & Assert
        assertThatThrownBy(() -> new ParticulateMatter(12, 18, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("pm10 must not be null");
    }
}
