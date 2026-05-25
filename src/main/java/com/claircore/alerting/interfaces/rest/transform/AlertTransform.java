package com.claircore.alerting.interfaces.rest.transform;

import com.claircore.alerting.domain.model.entities.Alert;
import com.claircore.alerting.interfaces.rest.resources.AlertResponse;

public final class AlertTransform {

    private AlertTransform() {
    }

    public static AlertResponse toResponse(Alert alert) {
        return AlertResponse.from(alert);
    }
}
