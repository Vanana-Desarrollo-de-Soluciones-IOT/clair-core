package com.claircore.analytics.domain.model.queries;

import java.util.UUID;

public record GetOverviewDashboardQuery(
        UUID ownerUserId,
        Integer deviceLimitPerSpace,
        Integer alertLimit
) {
    public GetOverviewDashboardQuery {
        if (ownerUserId == null) {
            throw new IllegalArgumentException("ownerUserId must not be null");
        }
        int deviceLimit = deviceLimitPerSpace != null ? deviceLimitPerSpace : 200;
        int alerts = alertLimit != null ? alertLimit : 5;

        if (deviceLimit <= 0 || deviceLimit > 500) {
            throw new IllegalArgumentException("deviceLimitPerSpace must be between 1 and 500");
        }
        if (alerts < 0 || alerts > 100) {
            throw new IllegalArgumentException("alertLimit must be between 0 and 100");
        }
        deviceLimitPerSpace = deviceLimit;
        alertLimit = alerts;
    }
}

