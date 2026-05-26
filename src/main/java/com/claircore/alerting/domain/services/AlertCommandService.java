package com.claircore.alerting.domain.services;

import com.claircore.alerting.domain.model.commands.EvaluateTelemetryForAlertsCommand;

/**
 * Command-side domain contract for alert generation.
 */
public interface AlertCommandService {

    void handle(EvaluateTelemetryForAlertsCommand command);
}
