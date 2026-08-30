package com.claircore.alerting.interfaces.rest.resources;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;

public record EdgeAlertAckRequest(@NotBlank String hardware_id, @NotNull Instant acknowledged_at) {}
