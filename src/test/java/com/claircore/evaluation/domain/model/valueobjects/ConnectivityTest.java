package com.claircore.evaluation.domain.model.valueobjects;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ConnectivityTest {

    @Test
    void shouldCreateConnectivityWhenValuesAreValid() {
        // Arrange & Act
        Connectivity connectivity = new Connectivity("ONLINE", "WiFi-Home", -65);

        // Assert
        assertThat(connectivity.status()).isEqualTo("ONLINE");
        assertThat(connectivity.network()).isEqualTo("WiFi-Home");
        assertThat(connectivity.signalStrength()).isEqualTo(-65);
    }

    @Test
    void shouldCreateConnectivityWhenNetworkAndSignalStrengthAreNull() {
        // Arrange & Act
        Connectivity connectivity = new Connectivity("OFFLINE", null, null);

        // Assert
        assertThat(connectivity.status()).isEqualTo("OFFLINE");
        assertThat(connectivity.network()).isNull();
        assertThat(connectivity.signalStrength()).isNull();
    }

    @Test
    void shouldThrowExceptionWhenStatusIsNull() {
        // Arrange, Act & Assert
        assertThatThrownBy(() -> new Connectivity(null, "WiFi", -50))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("status must not be null or blank");
    }

    @Test
    void shouldThrowExceptionWhenStatusIsBlank() {
        // Arrange, Act & Assert
        assertThatThrownBy(() -> new Connectivity("   ", "WiFi", -50))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("status must not be null or blank");
    }

    @Test
    void shouldThrowExceptionWhenSignalStrengthIsTooLow() {
        // Arrange, Act & Assert
        assertThatThrownBy(() -> new Connectivity("ONLINE", "WiFi", -151))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("signalStrength must be between -150 and 0");
    }

    @Test
    void shouldThrowExceptionWhenSignalStrengthIsTooHigh() {
        // Arrange, Act & Assert
        assertThatThrownBy(() -> new Connectivity("ONLINE", "WiFi", 1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("signalStrength must be between -150 and 0");
    }
}
