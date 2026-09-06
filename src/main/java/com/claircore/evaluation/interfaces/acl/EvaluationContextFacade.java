package com.claircore.evaluation.interfaces.acl;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EvaluationContextFacade {

    Optional<Instant> getLatestEvaluationRecordedAt(UUID deviceId);

    List<HourlyTelemetryAverage> getHourlyTelemetryAggregation(Instant start, Instant end);

    /** Every reading in [start, end), device then time ascending. */
    List<TelemetryReading> getReadingsBetween(Instant start, Instant end);
}
