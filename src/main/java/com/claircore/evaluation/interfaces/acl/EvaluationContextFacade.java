package com.claircore.evaluation.interfaces.acl;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public interface EvaluationContextFacade {

    Optional<Instant> getLatestEvaluationRecordedAt(UUID deviceId);

    List<Map<String, Object>> getHourlyTelemetryAggregation(Instant start, Instant end);
}
