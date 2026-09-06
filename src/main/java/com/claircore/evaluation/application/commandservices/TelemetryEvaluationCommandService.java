package com.claircore.evaluation.application.commandservices;

import com.claircore.evaluation.domain.model.aggregates.TelemetryEvaluation;
import com.claircore.evaluation.domain.model.commands.EvaluateTelemetryCommand;

public interface TelemetryEvaluationCommandService {
    TelemetryEvaluation handle(EvaluateTelemetryCommand command);
}
