package com.claircore.evaluation.domain.model.queries;

import org.junit.jupiter.api.Test;
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GetEvaluationsByDeviceQueryTest {

    @Test
    void shouldCreateQueryWhenDeviceIdIsValid() {
        // Arrange
        UUID deviceId = UUID.randomUUID();

        // Act
        GetEvaluationsByDeviceQuery query = new GetEvaluationsByDeviceQuery(deviceId, 0, 10);

        // Assert
        assertThat(query.deviceId()).isEqualTo(deviceId);
        assertThat(query.page()).isEqualTo(0);
        assertThat(query.size()).isEqualTo(10);
    }

    @Test
    void shouldThrowExceptionWhenDeviceIdIsNull() {
        // Arrange, Act & Assert
        assertThatThrownBy(() -> new GetEvaluationsByDeviceQuery(null, 0, 10))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Device ID must not be null");
    }
}
