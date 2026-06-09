package com.claircore.evaluation.domain.services;

import com.claircore.evaluation.domain.model.commands.EvaluateTelemetryCommand;
import com.claircore.evaluation.domain.model.entities.TelemetryEvaluation;

public interface TelemetryEvaluationCommandService {
    TelemetryEvaluation handle(EvaluateTelemetryCommand command);
}
