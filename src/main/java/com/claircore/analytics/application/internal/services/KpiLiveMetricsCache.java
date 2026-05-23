package com.claircore.analytics.application.internal.services;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.UUID;

@Component
public class KpiLiveMetricsCache {

    private final Cache<UUID, KpiLiveMetricsBuffer> cache = Caffeine.newBuilder()
            .expireAfterAccess(Duration.ofMinutes(10))
            .build();

    public KpiLiveMetricsBuffer getOrCreate(UUID deviceId) {
        return cache.get(deviceId, k -> new KpiLiveMetricsBuffer());
    }

    public KpiLiveMetricsBuffer getIfPresent(UUID deviceId) {
        return cache.getIfPresent(deviceId);
    }
}
