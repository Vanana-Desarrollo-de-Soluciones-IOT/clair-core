package com.claircore.alerting.interfaces.acl;

import com.claircore.alerting.domain.model.valueobjects.AlertStatus;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AlertingContextFacade {
    List<AlertDetailsDto> getActiveAlertsByDeviceId(UUID deviceId);
    Optional<AlertDetailsDto> getAlertDetailsById(UUID alertId);

    /**
     * Ownership-scoped alert summaries across all owned devices.
     */
    List<AlertDetailsDto> getRecentAlertsByOwnerId(UUID ownerUserId, List<AlertStatus> statuses, int limit);
}
