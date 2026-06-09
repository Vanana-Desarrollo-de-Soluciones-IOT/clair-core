package com.claircore.analytics.domain.services;

import com.claircore.analytics.domain.model.entities.DeviceMonthlySummary;
import com.claircore.analytics.domain.model.queries.GetMonthlyReportQuery;

import java.util.Optional;

public interface MonthlyReportQueryService {

    Optional<DeviceMonthlySummary> handle(GetMonthlyReportQuery query);
}
