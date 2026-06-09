package com.claircore.alerting.application.internal.outboundservices.acl;

import com.claircore.device.domain.model.valueobjects.DeviceMetricThresholdConfiguration;
import com.claircore.device.interfaces.acl.ThresholdContextFacade;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class ExternalAlertingThresholdService {

    private final ThresholdContextFacade thresholdContextFacade;

    public ExternalAlertingThresholdService(ThresholdContextFacade thresholdContextFacade) {
        this.thresholdContextFacade = thresholdContextFacade;
    }

    public List<DeviceMetricThresholdConfiguration> fetchEnabledThresholdsByDeviceId(UUID deviceId) {
        return thresholdContextFacade.findEnabledThresholdsByDeviceId(deviceId);
    }
}
