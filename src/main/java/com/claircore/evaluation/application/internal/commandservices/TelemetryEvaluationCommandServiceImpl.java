package com.claircore.evaluation.application.internal.commandservices;

import com.claircore.evaluation.application.commandservices.TelemetryEvaluationCommandService;
import com.claircore.evaluation.domain.model.aggregates.TelemetryEvaluation;
import com.claircore.evaluation.domain.model.commands.EvaluateTelemetryCommand;
import com.claircore.evaluation.domain.repositories.TelemetryEvaluationRepository;
import com.claircore.evaluation.interfaces.events.TelemetryRecordedIntegrationEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TelemetryEvaluationCommandServiceImpl implements TelemetryEvaluationCommandService {

    private final TelemetryEvaluationRepository telemetryEvaluationRepository;
    private final ApplicationEventPublisher eventPublisher;

    public TelemetryEvaluationCommandServiceImpl(
            TelemetryEvaluationRepository telemetryEvaluationRepository,
            ApplicationEventPublisher eventPublisher
    ) {
        this.telemetryEvaluationRepository = telemetryEvaluationRepository;
        this.eventPublisher = eventPublisher;
    }

    @Override
    @Transactional
    public TelemetryEvaluation handle(EvaluateTelemetryCommand command) {
        var evaluation = new TelemetryEvaluation(
                command.deviceId(),
                command.deviceTime(),
                command.uptime(),
                command.airQuality(),
                command.particulateMatter(),
                command.connectivity(),
                command.location(),
                command.healthStatus(),
                command.status(),
                command.recordedAt()
        );

        TelemetryEvaluation saved = telemetryEvaluationRepository.save(evaluation);

        // The published contract, and now the only telemetry event: alerting and analytics both
        // listen to it, so the internal event this used to be published alongside is gone.
        eventPublisher.publishEvent(TelemetryRecordedIntegrationEvent.from(saved));

        return saved;
    }
}
