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
    /** Alerting reports that it finished with a reading; the receipt stops the catch-up replay. */
    void markAlertsEvaluated(UUID deviceId, UUID readingId);
}
