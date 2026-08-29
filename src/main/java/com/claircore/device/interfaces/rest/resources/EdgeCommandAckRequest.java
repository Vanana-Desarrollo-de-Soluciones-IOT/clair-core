package com.claircore.device.interfaces.rest.resources;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;

public record EdgeCommandAckRequest(
        @NotBlank String hardware_id,
        @NotNull Instant acknowledged_at,
        @NotNull Result result,
        String detail
) {
    public enum Result { OK, FAILED }
}
