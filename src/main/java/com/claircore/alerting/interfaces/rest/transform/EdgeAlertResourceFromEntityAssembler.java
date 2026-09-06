package com.claircore.alerting.interfaces.rest.transform;

import com.claircore.alerting.application.queryservices.AlertQueryService;
import com.claircore.alerting.interfaces.rest.resources.EdgeAlertResource;

public final class EdgeAlertResourceFromEntityAssembler {

    private EdgeAlertResourceFromEntityAssembler() {
    }

    public static EdgeAlertResource toResourceFromEntity(AlertQueryService.PendingEdgeAlert pending) {
        var alert = pending.alert();
        return new EdgeAlertResource(
                alert.getId().toString(),
                alert.getDeviceId().toString(),
                pending.hardwareId(),
                alert.getMetric().name(),
                alert.getThresholdValue(),
                alert.getActualValue(),
                alert.getMessage(),
                alert.getStatus().name(),
                alert.getOccurredAt().toString(),
                alert.getResolvedAt() == null ? null : alert.getResolvedAt().toString());
    }
}
