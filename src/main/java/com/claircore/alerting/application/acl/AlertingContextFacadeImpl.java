package com.claircore.alerting.application.acl;

import com.claircore.alerting.application.internal.outboundservices.acl.ExternalAlertingDeviceService;
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
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

@Service
public class AlertingContextFacadeImpl implements AlertingContextFacade {

    private final AlertRepository alertRepository;
    private final ExternalAlertingDeviceService externalDeviceService;

    public AlertingContextFacadeImpl(
            AlertRepository alertRepository,
            ExternalAlertingDeviceService externalDeviceService
    ) {
        this.alertRepository = alertRepository;
        this.externalDeviceService = externalDeviceService;
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

    @Override
    @Transactional(readOnly = true)
    public List<AlertDetailsDto> getRecentAlertsByOwnerId(UUID ownerUserId, List<AlertStatus> statuses, int limit) {
        if (ownerUserId == null) return List.of();
        int size = Math.max(0, limit);
        if (size == 0) return List.of();

        List<UUID> ownerDeviceIds = externalDeviceService.fetchDeviceIdsByOwnerId(ownerUserId);
        if (ownerDeviceIds == null || ownerDeviceIds.isEmpty()) return List.of();

        var pageable = PageRequest.of(0, size, Sort.by(Sort.Direction.DESC, "occurredAt"));
        var page = (statuses != null && !statuses.isEmpty())
                ? alertRepository.findByDeviceIdInAndStatusIn(ownerDeviceIds, statuses, pageable)
                : alertRepository.findByDeviceIdIn(ownerDeviceIds, pageable);

        return page.getContent().stream().map(this::toDto).toList();
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
