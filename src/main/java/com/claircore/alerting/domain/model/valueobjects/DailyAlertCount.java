package com.claircore.alerting.domain.model.valueobjects;

import java.time.LocalDate;

public record DailyAlertCount(LocalDate date, long count) {
    public DailyAlertCount {
        if (date == null) {
            throw new IllegalArgumentException("Date must not be null");
        }
        if (count < 0) {
            throw new IllegalArgumentException("Count must not be negative");
        }
    }
}
