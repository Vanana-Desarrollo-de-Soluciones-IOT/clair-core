package com.claircore.alerting.interfaces.acl;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AlertingContextFacade {
    List<AlertDetailsDto> getActiveAlertsByDeviceId(UUID deviceId);
    Optional<AlertDetailsDto> getAlertDetailsById(UUID alertId);
}
