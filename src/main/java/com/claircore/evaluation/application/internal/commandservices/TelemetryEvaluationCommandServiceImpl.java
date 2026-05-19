package com.claircore.evaluation.application.internal.commandservices;

import com.claircore.evaluation.domain.model.commands.EvaluateTelemetryCommand;
import com.claircore.evaluation.domain.model.entities.TelemetryEvaluation;
import com.claircore.evaluation.domain.services.TelemetryEvaluationCommandService;
import com.claircore.evaluation.infrastructure.persistence.jpa.repositories.TelemetryEvaluationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TelemetryEvaluationCommandServiceImpl implements TelemetryEvaluationCommandService {

    private final TelemetryEvaluationRepository telemetryEvaluationRepository;

    public TelemetryEvaluationCommandServiceImpl(TelemetryEvaluationRepository telemetryEvaluationRepository) {
        this.telemetryEvaluationRepository = telemetryEvaluationRepository;
    }

    @Override
    @Transactional
    public TelemetryEvaluation handle(EvaluateTelemetryCommand command) {
        var evaluation = new TelemetryEvaluation(
                command.deviceId(),
                command.deviceTimestamp(),
                command.uptimeSeconds(),
                command.airQuality(),
                command.particulateMatter(),
                command.connectivity(),
                command.deviceHealth(),
                command.deviceInfo(),
                command.status(),
                command.statusCode(),
                command.recordedAt()
        );

        return telemetryEvaluationRepository.save(evaluation);
    }
}
