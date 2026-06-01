package com.claircore.analytics.application.internal.commandservices;

import com.claircore.analytics.application.internal.services.KpiLiveMetricsCache;
import com.claircore.analytics.domain.model.commands.ProcessTelemetryAnalyticCommand;
import com.claircore.analytics.domain.model.events.TelemetryReceivedEvent;
import com.claircore.analytics.domain.services.KpiLiveMetricsCommandService;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

@Service
public class KpiLiveMetricsCommandServiceImpl implements KpiLiveMetricsCommandService {

    private final KpiLiveMetricsCache liveMetricsCache;
    private final ApplicationEventPublisher eventPublisher;

    public KpiLiveMetricsCommandServiceImpl(
            KpiLiveMetricsCache liveMetricsCache,
            ApplicationEventPublisher eventPublisher
    ) {
        this.liveMetricsCache = liveMetricsCache;
        this.eventPublisher = eventPublisher;
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

        eventPublisher.publishEvent(new TelemetryReceivedEvent(
                command.deviceId().value(),
                command.co2(),
                command.pm2_5(),
                command.temperature(),
                command.humidity(),
                command.recordedAt()
        ));
    }
}
