package com.claircore.analytics.application.internal.commandservices;

import com.claircore.analytics.application.internal.services.KpiLiveMetricsCache;
import com.claircore.analytics.domain.model.commands.ProcessTelemetryAnalyticCommand;
import com.claircore.analytics.domain.services.KpiLiveMetricsCommandService;
import org.springframework.stereotype.Service;

@Service
public class KpiLiveMetricsCommandServiceImpl implements KpiLiveMetricsCommandService {

    private final KpiLiveMetricsCache liveMetricsCache;

    public KpiLiveMetricsCommandServiceImpl(KpiLiveMetricsCache liveMetricsCache) {
        this.liveMetricsCache = liveMetricsCache;
    }

    @Override
    public void handle(ProcessTelemetryAnalyticCommand command) {
        var buffer = liveMetricsCache.getOrCreate(command.deviceId().value());
        buffer.add(
                command.recordedAt(),
                command.co2(),
                command.pm2_5(),
                command.temperature(),
                command.humidity()
        );
    }
}
