package com.claircore.analytics.domain.exceptions;

import java.util.UUID;

public class DeviceTelemetryUnavailableException extends RuntimeException {
    public DeviceTelemetryUnavailableException(UUID deviceId, boolean isLive) {
        super(isLive 
            ? String.format("Device with ID %s has no recent live telemetry data; it might be turned off or disconnected.", deviceId)
            : String.format("No telemetry data available for device with ID %s in the requested period.", deviceId)
        );
    }
}
