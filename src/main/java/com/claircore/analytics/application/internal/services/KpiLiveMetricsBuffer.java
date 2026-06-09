package com.claircore.analytics.application.internal.services;

import java.time.Instant;
import java.time.Duration;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class KpiLiveMetricsBuffer {

    private final Queue<Reading> readings = new ConcurrentLinkedQueue<>();
    private static final Duration WINDOW = Duration.ofMinutes(5);

    public void add(Instant timestamp, double co2, double pm2_5, double temperature, double humidity) {
        readings.add(new Reading(timestamp, co2, pm2_5, temperature, humidity));
        prune();
    }

    public boolean isEmpty() {
        prune();
        return readings.isEmpty();
    }

    private void prune() {
        Instant cutoff = Instant.now().minus(WINDOW);
        readings.removeIf(r -> r.timestamp().isBefore(cutoff));
    }

    public Averages computeAverages() {
        prune();
        if (readings.isEmpty()) {
            return new Averages(0.0, 0.0, 0.0, 0.0);
        }
        double sumCo2 = 0.0;
        double sumPm25 = 0.0;
        double sumTemp = 0.0;
        double sumHum = 0.0;
        int count = readings.size();
        for (Reading r : readings) {
            sumCo2 += r.co2();
            sumPm25 += r.pm2_5();
            sumTemp += r.temperature();
            sumHum += r.humidity();
        }
        return new Averages(sumCo2 / count, sumPm25 / count, sumTemp / count, sumHum / count);
    }

    public record Reading(Instant timestamp, double co2, double pm2_5, double temperature, double humidity) {}

    public record Averages(double co2, double pm2_5, double temperature, double humidity) {}
}
