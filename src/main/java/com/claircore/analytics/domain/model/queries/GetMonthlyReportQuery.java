package com.claircore.analytics.domain.model.queries;

import com.claircore.analytics.domain.model.valueobjects.DeviceId;

import java.time.LocalDate;

/**
 * Request for a device's monthly report. {@code month} is normalised to the
 * first day of the month. Premium-gated at the interface layer.
 */
public record GetMonthlyReportQuery(DeviceId deviceId, LocalDate month) {
    public GetMonthlyReportQuery {
        if (deviceId == null) {
            throw new IllegalArgumentException("deviceId must not be null");
        }
        if (month == null) {
            throw new IllegalArgumentException("month must not be null");
        }
    }
}
