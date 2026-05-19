package com.claircore.evaluation.application.internal.commandservices;

import com.claircore.evaluation.domain.model.commands.EvaluateTelemetryCommand;
import com.claircore.evaluation.domain.model.entities.TelemetryEvaluation;
import com.claircore.evaluation.domain.services.TelemetryEvaluationCommandService;
import com.claircore.evaluation.application.internal.outboundservices.acl.ExternalDeviceService;
import com.claircore.evaluation.infrastructure.persistence.jpa.repositories.TelemetryEvaluationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TelemetryEvaluationCommandServiceImpl implements TelemetryEvaluationCommandService {

    private final TelemetryEvaluationRepository telemetryEvaluationRepository;
    private final ExternalDeviceService externalDeviceService;

    public TelemetryEvaluationCommandServiceImpl(
            TelemetryEvaluationRepository telemetryEvaluationRepository,
            ExternalDeviceService externalDeviceService
    ) {
        this.telemetryEvaluationRepository = telemetryEvaluationRepository;
        this.externalDeviceService = externalDeviceService;
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
                command.status(),
                command.recordedAt()
        );

        TelemetryEvaluation savedEvaluation = telemetryEvaluationRepository.save(evaluation);
        externalDeviceService.markDeviceSeen(command.deviceId());
        return savedEvaluation;
    }
}
