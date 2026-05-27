package com.claircore.alerting.application.acl;

import com.claircore.alerting.domain.model.entities.Alert;
import com.claircore.alerting.domain.model.valueobjects.AlertStatus;
import com.claircore.alerting.infrastructure.persistence.jpa.repositories.AlertRepository;
import com.claircore.alerting.interfaces.acl.AlertDetailsDto;
import com.claircore.alerting.interfaces.acl.AlertingContextFacade;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class AlertingContextFacadeImpl implements AlertingContextFacade {

    private final AlertRepository alertRepository;

    public AlertingContextFacadeImpl(AlertRepository alertRepository) {
        this.alertRepository = alertRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<AlertDetailsDto> getActiveAlertsByDeviceId(UUID deviceId) {
        List<Alert> activeAlerts = alertRepository.findByDeviceIdAndStatus(deviceId, AlertStatus.ACTIVE);
        return activeAlerts.stream().map(this::toDto).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<AlertDetailsDto> getAlertDetailsById(UUID alertId) {
        return alertRepository.findById(alertId).map(this::toDto);
    }

    private AlertDetailsDto toDto(Alert alert) {
        return new AlertDetailsDto(
                alert.getId(),
                alert.getDeviceId(),
                alert.getSpaceId(),
                alert.getDeviceName(),
                alert.getMetric().name(),
                alert.getThresholdValue(),
                alert.getActualValue(),
                alert.getMessage(),
                alert.getStatus().name(),
                alert.getSeverity().name(),
                alert.getOccurredAt()
        );
    }
}
