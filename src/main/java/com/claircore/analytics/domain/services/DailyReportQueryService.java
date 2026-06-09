package com.claircore.analytics.domain.services;

import com.claircore.analytics.domain.model.entities.DeviceDailySummary;
import com.claircore.analytics.domain.model.queries.GetDailyReportQuery;

import java.util.Optional;

public interface DailyReportQueryService {

    Optional<DeviceDailySummary> handle(GetDailyReportQuery query);
}
