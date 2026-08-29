package com.claircore.alerting.application.internal.commandservices;

import com.claircore.alerting.domain.model.entities.Alert;
import com.claircore.alerting.domain.model.valueobjects.AlertStatus;
import com.claircore.alerting.infrastructure.persistence.jpa.repositories.AlertRepository;
import com.claircore.alerting.interfaces.rest.resources.EdgeAlertAckRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/** Atomically applies acknowledgements received from the edge. */
@Service
public class EdgeAlertAcknowledgementService {
    public enum Outcome { NOT_FOUND, CONFLICT, OK }

    private final AlertRepository repository;

    public EdgeAlertAcknowledgementService(AlertRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public Outcome acknowledge(UUID alertId, EdgeAlertAckRequest request) {
        return repository.findByIdForAcknowledgement(alertId)
                .map(alert -> acknowledgeLoaded(alert, request))
                .orElse(Outcome.NOT_FOUND);
    }

    private Outcome acknowledgeLoaded(Alert alert, EdgeAlertAckRequest request) {
        // Check ownership first so an edge cannot learn the lifecycle state of an
        // alert belonging to another hardware identity.
        if (!repository.findHardwareIdByAlertId(alert.getId())
                .map(request.hardware_id()::equals).orElse(false)) {
            return Outcome.NOT_FOUND;
        }
        if (alert.getStatus() == AlertStatus.ACKNOWLEDGED || alert.getStatus() == AlertStatus.RESOLVED) {
            return Outcome.CONFLICT;
        }
        if (alert.getStatus() != AlertStatus.ACTIVE) {
            return Outcome.NOT_FOUND;
        }
        alert.acknowledge();
        repository.save(alert);
        return Outcome.OK;
    }
}
