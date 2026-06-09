package com.claircore.analytics.domain.model.queries;

import com.claircore.analytics.domain.model.valueobjects.DeviceId;

import java.time.LocalDate;

/**
 * Request for a device's daily report. A null {@code date} means "the latest
 * completed day". Only completed days are served (no in-progress period).
 */
public record GetDailyReportQuery(DeviceId deviceId, LocalDate date) {
    public GetDailyReportQuery {
        if (deviceId == null) {
            throw new IllegalArgumentException("deviceId must not be null");
        }
    }
}
