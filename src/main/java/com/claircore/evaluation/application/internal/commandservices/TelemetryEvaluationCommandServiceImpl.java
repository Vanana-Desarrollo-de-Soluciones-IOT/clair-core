package com.claircore.evaluation.application.internal.commandservices;

import com.claircore.evaluation.application.commandservices.TelemetryEvaluationCommandService;
import com.claircore.evaluation.domain.model.aggregates.TelemetryEvaluation;
import com.claircore.evaluation.domain.model.commands.EvaluateTelemetryCommand;
import com.claircore.evaluation.domain.model.events.TelemetryRecordedEvent;
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

        // The internal domain event alerting and analytics consume today.
        eventPublisher.publishEvent(new TelemetryRecordedEvent(
                command.deviceId().value(),
                null, // hardwareId not required as deviceId is already resolved
                command.airQuality().co2(),
                command.particulateMatter().pm2_5(),
                command.airQuality().temperature(),
                command.airQuality().humidity(),
                command.recordedAt(),
                command.recordedAt(),
                command.particulateMatter().pm10(),
                command.particulateMatter().pm1_0(),
                command.connectivity().status(),
                command.connectivity().network(),
                command.connectivity().signalStrength(),
                command.location().country(),
                command.healthStatus().toString(),
                command.status(),
                command.uptime(),
                command.deviceTime().toString()
        ));

        // The published contract. No consumers yet; alerting and analytics move onto it when they
        // are split, and the internal event above stops being published then.
        eventPublisher.publishEvent(TelemetryRecordedIntegrationEvent.from(saved));

        return saved;
    }
}
