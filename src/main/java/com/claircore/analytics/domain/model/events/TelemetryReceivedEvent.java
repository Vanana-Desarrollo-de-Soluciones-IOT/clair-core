package com.claircore.analytics.domain.model.events;

import java.time.Instant;
import java.util.UUID;

public record TelemetryReceivedEvent(
        UUID deviceId,
        double co2,
        double pm2_5,
        double temperature,
        double humidity,
        Instant timestamp
) {}
