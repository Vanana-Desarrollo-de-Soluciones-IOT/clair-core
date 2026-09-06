package com.claircore.alerting.application.commandservices;

import com.claircore.alerting.domain.model.commands.AcknowledgeEdgeAlertCommand;
import com.claircore.alerting.domain.model.commands.EvaluateTelemetryForAlertsCommand;

/** Inbound port for the alerting write side. */
public interface AlertCommandService {

    void handle(EvaluateTelemetryForAlertsCommand command);

    /** Outcome of an edge acknowledgement, so the REST layer maps a result rather than an exception. */
    enum AcknowledgementOutcome { NOT_FOUND, CONFLICT, OK }

    AcknowledgementOutcome handle(AcknowledgeEdgeAlertCommand command);
}
