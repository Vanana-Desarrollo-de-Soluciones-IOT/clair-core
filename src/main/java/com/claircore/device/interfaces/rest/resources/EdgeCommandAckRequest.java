package com.claircore.device.interfaces.rest.resources;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record EdgeCommandAckRequest(
        @NotBlank String hardware_id,
        @NotNull Result result,
        String detail
) {
    public enum Result { OK, FAILED }
}
