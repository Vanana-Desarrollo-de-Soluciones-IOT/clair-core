package com.claircore.device.interfaces.rest.resources;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.UUID;

public record EdgePresenceRequest(@NotNull UUID device_id, @NotBlank String hardware_id, @NotBlank String status, @NotNull Instant occurred_at) {}
