package com.claircore.evaluation.domain.model.queries;

import org.junit.jupiter.api.Test;
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GetLatestEvaluationByDeviceQueryTest {

    @Test
    void shouldCreateQueryWhenDeviceIdIsValid() {
        // Arrange
        UUID deviceId = UUID.randomUUID();

        // Act
        GetLatestEvaluationByDeviceQuery query = new GetLatestEvaluationByDeviceQuery(deviceId);

        // Assert
        assertThat(query.deviceId()).isEqualTo(deviceId);
    }

    @Test
    void shouldThrowExceptionWhenDeviceIdIsNull() {
        // Arrange, Act & Assert
        assertThatThrownBy(() -> new GetLatestEvaluationByDeviceQuery(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Device ID must not be null");
    }
}
