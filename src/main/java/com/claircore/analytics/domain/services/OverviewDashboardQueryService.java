package com.claircore.analytics.domain.services;

import com.claircore.analytics.domain.model.queries.GetOverviewDashboardQuery;
import com.claircore.analytics.domain.model.valueobjects.OverviewDashboardSnapshot;

public interface OverviewDashboardQueryService {
    OverviewDashboardSnapshot handle(GetOverviewDashboardQuery query);
}

