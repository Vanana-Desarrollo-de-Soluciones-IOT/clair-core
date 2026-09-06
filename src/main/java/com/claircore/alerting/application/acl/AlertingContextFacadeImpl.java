package com.claircore.alerting.application.acl;

import com.claircore.alerting.application.internal.outboundservices.acl.ExternalAlertingDeviceService;
import com.claircore.alerting.domain.model.aggregates.Alert;
import com.claircore.alerting.domain.model.valueobjects.AlertStatus;
import com.claircore.alerting.domain.repositories.AlertRepository;
import com.claircore.alerting.interfaces.acl.AlertDetails;
import com.claircore.alerting.interfaces.acl.AlertingContextFacade;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

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
    public List<AlertDetails> getActiveAlertsByDeviceId(UUID deviceId) {
        return alertRepository.findByDeviceIdAndStatus(deviceId, AlertStatus.ACTIVE).stream()
                .map(AlertingContextFacadeImpl::toDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<AlertDetails> getAlertDetailsById(UUID alertId) {
        return alertRepository.findById(alertId).map(AlertingContextFacadeImpl::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AlertDetails> getRecentAlertsByOwnerId(UUID ownerUserId, List<String> statuses, int limit) {
        if (ownerUserId == null) return List.of();
        int size = Math.max(0, limit);
        if (size == 0) return List.of();

        List<UUID> ownerDeviceIds = externalDeviceService.fetchDeviceIdsByOwnerId(ownerUserId);
        if (ownerDeviceIds == null || ownerDeviceIds.isEmpty()) return List.of();

        List<AlertStatus> parsed = parseStatuses(statuses);
        var page = parsed.isEmpty()
                ? alertRepository.findByDeviceIdIn(ownerDeviceIds, 0, size)
                : alertRepository.findByDeviceIdInAndStatusIn(ownerDeviceIds, parsed, 0, size);

        return page.items().stream().map(AlertingContextFacadeImpl::toDto).toList();
    }

    /** An unrecognised name is dropped rather than throwing: the caller is another context. */
    private static List<AlertStatus> parseStatuses(List<String> statuses) {
        if (statuses == null) return List.of();
        return statuses.stream()
                .map(AlertingContextFacadeImpl::parseStatus)
                .flatMap(Optional::stream)
                .toList();
    }

    private static Optional<AlertStatus> parseStatus(String name) {
        if (name == null || name.isBlank()) return Optional.empty();
        try {
            return Optional.of(AlertStatus.valueOf(name));
        } catch (IllegalArgumentException ignored) {
            return Optional.empty();
        }
    }

    private static AlertDetails toDto(Alert alert) {
        return new AlertDetails(
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
