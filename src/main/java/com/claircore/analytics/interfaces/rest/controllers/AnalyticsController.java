package com.claircore.analytics.interfaces.rest.controllers;

import com.claircore.analytics.domain.model.queries.GetHistoricalTrendQuery;
import com.claircore.analytics.domain.model.queries.GetLiveDashboardMetricsQuery;
import com.claircore.analytics.domain.model.valueobjects.DeviceId;
import com.claircore.analytics.domain.model.valueobjects.TrendPeriod;
import com.claircore.analytics.domain.services.KpiDashboardMetricsQueryService;
import com.claircore.analytics.domain.services.KpiHistoricalTrendQueryService;
import com.claircore.analytics.interfaces.rest.resources.DashboardMetricsResponse;
import com.claircore.analytics.interfaces.rest.resources.TrendChartResponse;
import com.claircore.analytics.interfaces.rest.transform.AnalyticsTransform;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/analytics")
@Tag(name = "Analytics", description = "KPI and trend analytics endpoints")
public class AnalyticsController {

    private final KpiDashboardMetricsQueryService kpiDashboardMetricsQueryService;
    private final KpiHistoricalTrendQueryService kpiHistoricalTrendQueryService;

    public AnalyticsController(
            KpiDashboardMetricsQueryService kpiDashboardMetricsQueryService,
            KpiHistoricalTrendQueryService kpiHistoricalTrendQueryService
    ) {
        this.kpiDashboardMetricsQueryService = kpiDashboardMetricsQueryService;
        this.kpiHistoricalTrendQueryService = kpiHistoricalTrendQueryService;
    }

    @GetMapping("/devices/{deviceId}/live")
    @Operation(summary = "Get live dashboard KPI metrics for a device")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Live metrics returned"),
            @ApiResponse(responseCode = "404", description = "No live data available for device")
    })
    public ResponseEntity<DashboardMetricsResponse> getLiveMetrics(
            @Parameter(description = "Device UUID", example = "a1b2c3d4-e5f6-7890-abcd-ef1234567890")
            @PathVariable UUID deviceId
    ) {
        var query = new GetLiveDashboardMetricsQuery(new DeviceId(deviceId));
        return kpiDashboardMetricsQueryService.handle(query)
                .map(AnalyticsTransform::toDashboardResponse)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/devices/{deviceId}/trends")
    @Operation(summary = "Get historical trend chart data for a device")
    @ApiResponse(responseCode = "200", description = "Trend data returned")
    public ResponseEntity<TrendChartResponse> getTrends(
            @Parameter(description = "Device UUID", example = "a1b2c3d4-e5f6-7890-abcd-ef1234567890")
            @PathVariable UUID deviceId,
            @Parameter(description = "Period filter", example = "DAY")
            @RequestParam(defaultValue = "DAY") String period
    ) {
        TrendPeriod trendPeriod;
        try {
            trendPeriod = TrendPeriod.valueOf(period.toUpperCase());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }

        var query = new GetHistoricalTrendQuery(new DeviceId(deviceId), trendPeriod);
        var points = kpiHistoricalTrendQueryService.handle(query);
        return ResponseEntity.ok(AnalyticsTransform.toTrendChartResponse(points));
    }
}
