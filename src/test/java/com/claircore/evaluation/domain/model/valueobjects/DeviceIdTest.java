package com.claircore.evaluation.domain.model.valueobjects;

import org.junit.jupiter.api.Test;
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DeviceIdTest {

    @Test
    void shouldCreateDeviceIdWhenValueIsValid() {
        // Arrange
        UUID uuid = UUID.randomUUID();

        // Act
        DeviceId deviceId = new DeviceId(uuid);

        // Assert
        assertThat(deviceId.value()).isEqualTo(uuid);
    }

    @Test
    void shouldThrowExceptionWhenValueIsNull() {
        // Arrange, Act & Assert
        assertThatThrownBy(() -> new DeviceId(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Device ID must not be null");
    }
}
