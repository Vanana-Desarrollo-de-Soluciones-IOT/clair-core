package com.claircore.analytics.domain.services;

import com.claircore.analytics.domain.model.commands.ProcessTelemetryAnalyticCommand;

public interface KpiLiveMetricsCommandService {

    void handle(ProcessTelemetryAnalyticCommand command);
}
