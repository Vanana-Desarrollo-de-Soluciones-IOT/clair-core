package com.claircore.notifications.application.internal.outboundservices.acl;

import com.claircore.alerting.interfaces.acl.AlertDetailsDto;
import com.claircore.alerting.interfaces.acl.AlertingContextFacade;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service("notificationsExternalAlertingService")
public class ExternalAlertingService {

    private final AlertingContextFacade alertingContextFacade;

    public ExternalAlertingService(AlertingContextFacade alertingContextFacade) {
        this.alertingContextFacade = alertingContextFacade;
    }

    public List<AlertDetailsDto> fetchActiveAlertsByDeviceId(UUID deviceId) {
        return alertingContextFacade.getActiveAlertsByDeviceId(deviceId);
    }

    public Optional<AlertDetailsDto> fetchAlertDetailsById(UUID alertId) {
        return alertingContextFacade.getAlertDetailsById(alertId);
    }
}
