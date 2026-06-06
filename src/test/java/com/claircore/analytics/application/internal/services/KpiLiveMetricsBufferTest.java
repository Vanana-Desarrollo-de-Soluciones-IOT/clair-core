package com.claircore.analytics.application.internal.services;

import org.junit.jupiter.api.Test;
import java.time.Instant;
import java.time.Duration;
import static org.junit.jupiter.api.Assertions.*;

class KpiLiveMetricsBufferTest {

    @Test
    void shouldComputeAveragesCorrectly() {
        var buffer = new KpiLiveMetricsBuffer();
        var now = Instant.now();

        buffer.add(now.minusSeconds(10), 400.0, 10.0, 20.0, 40.0);
        buffer.add(now, 600.0, 20.0, 30.0, 60.0);

        var averages = buffer.computeAverages();
        assertEquals(500.0, averages.co2());
        assertEquals(15.0, averages.pm2_5());
        assertEquals(25.0, averages.temperature());
        assertEquals(50.0, averages.humidity());
    }

    @Test
    void shouldPruneOldReadingsWhenAddingNewReading() {
        var buffer = new KpiLiveMetricsBuffer();
        var now = Instant.now();

        // 6 minutes ago (should be pruned)
        buffer.add(now.minus(Duration.ofMinutes(6)), 1000.0, 50.0, 25.0, 70.0);
        // 10 seconds ago (should remain)
        buffer.add(now.minusSeconds(10), 400.0, 10.0, 20.0, 40.0);

        var averages = buffer.computeAverages();
        // Since the 6 minutes ago reading is pruned, the average should only reflect the 10 seconds ago reading
        assertEquals(400.0, averages.co2());
        assertEquals(10.0, averages.pm2_5());
        assertEquals(20.0, averages.temperature());
        assertEquals(40.0, averages.humidity());
    }

    @Test
    void shouldReturnZeroAveragesWhenEmpty() {
        var buffer = new KpiLiveMetricsBuffer();
        var averages = buffer.computeAverages();
        assertEquals(0.0, averages.co2());
        assertEquals(0.0, averages.pm2_5());
        assertEquals(0.0, averages.temperature());
        assertEquals(0.0, averages.humidity());
    }
}
