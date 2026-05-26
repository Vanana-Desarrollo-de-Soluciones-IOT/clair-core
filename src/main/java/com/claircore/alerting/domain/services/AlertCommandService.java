package com.claircore.alerting.domain.services;

import com.claircore.alerting.domain.model.commands.EvaluateTelemetryForAlertsCommand;
import com.claircore.alerting.domain.model.commands.RecordAlertConditionStateChangedCommand;

/**
 * Command-side domain contract for alert generation.
 */
public interface AlertCommandService {

    void handle(EvaluateTelemetryForAlertsCommand command);

    void handle(RecordAlertConditionStateChangedCommand command);
}
