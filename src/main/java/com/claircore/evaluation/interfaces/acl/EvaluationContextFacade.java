package com.claircore.evaluation.interfaces.acl;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface EvaluationContextFacade {

    Optional<Instant> getLatestEvaluationRecordedAt(UUID deviceId);
}
