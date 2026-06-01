package com.claircore.analytics.interfaces.rest.controllers;

import com.claircore.analytics.domain.model.queries.GetHistoricalTrendQuery;
import com.claircore.analytics.domain.model.queries.GetDashboardMetricsQuery;
import com.claircore.analytics.domain.model.valueobjects.DeviceId;
import com.claircore.analytics.domain.model.valueobjects.TrendPeriod;
import com.claircore.analytics.domain.services.KpiDashboardMetricsQueryService;
import com.claircore.analytics.domain.services.KpiHistoricalTrendQueryService;
import com.claircore.analytics.domain.exceptions.DeviceTelemetryUnavailableException;
import com.claircore.analytics.interfaces.rest.resources.DashboardMetricsResponse;
import com.claircore.analytics.interfaces.rest.resources.TrendChartResponse;
import com.claircore.analytics.interfaces.rest.transform.AnalyticsTransform;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import com.claircore.analytics.application.internal.services.AnalyticsSseService;
import org.springframework.format.annotation.DateTimeFormat;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;

import java.time.Instant;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/analytics")
@Tag(name = "Analytics", description = "KPI and trend analytics endpoints")
public class AnalyticsController {

    private final KpiDashboardMetricsQueryService kpiDashboardMetricsQueryService;
    private final KpiHistoricalTrendQueryService kpiHistoricalTrendQueryService;
    private final AnalyticsSseService analyticsSseService;

    public AnalyticsController(
            KpiDashboardMetricsQueryService kpiDashboardMetricsQueryService,
            KpiHistoricalTrendQueryService kpiHistoricalTrendQueryService,
            AnalyticsSseService analyticsSseService
    ) {
        this.kpiDashboardMetricsQueryService = kpiDashboardMetricsQueryService;
        this.kpiHistoricalTrendQueryService = kpiHistoricalTrendQueryService;
        this.analyticsSseService = analyticsSseService;
    }

    @GetMapping("/devices/{deviceId}/live")
    @Operation(summary = "Get live dashboard KPI metrics for a device")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Dashboard metrics returned"),
            @ApiResponse(responseCode = "404", description = "No data available for device")
    })
    public ResponseEntity<DashboardMetricsResponse> getLiveMetrics(
            @Parameter(description = "Device UUID", example = "a1b2c3d4-e5f6-7890-abcd-ef1234567890")
            @PathVariable @NotNull UUID deviceId
    ) {
        var query = new GetDashboardMetricsQuery(new DeviceId(deviceId), "LIVE", null, null);
        return kpiDashboardMetricsQueryService.handle(query)
                .map(AnalyticsTransform::toDashboardResponse)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new DeviceTelemetryUnavailableException(deviceId, true));
    }

    @GetMapping(value = "/devices/{deviceId}/live/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "Stream live telemetry updates for a device using Server-Sent Events (SSE)")
    @ApiResponse(responseCode = "200", description = "SSE stream established")
    public SseEmitter streamLiveMetrics(
            @Parameter(description = "Device UUID", example = "a1b2c3d4-e5f6-7890-abcd-ef1234567890")
            @PathVariable @NotNull UUID deviceId
    ) {
        return analyticsSseService.registerClient(deviceId);
    }

    @GetMapping("/devices/{deviceId}/historical")
    @Operation(summary = "Get historical dashboard KPI metrics for a device")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Dashboard metrics returned"),
            @ApiResponse(responseCode = "400", description = "Invalid parameters"),
            @ApiResponse(responseCode = "404", description = "No data available for device")
    })
    public ResponseEntity<DashboardMetricsResponse> getHistoricalMetrics(
            @Parameter(description = "Device UUID", example = "a1b2c3d4-e5f6-7890-abcd-ef1234567890")
            @PathVariable @NotNull UUID deviceId,
            @RequestParam(required = false) String period,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) @PastOrPresent Instant startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) @PastOrPresent Instant endDate
    ) {
        if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
            return ResponseEntity.badRequest().build();
        }
        var query = new GetDashboardMetricsQuery(new DeviceId(deviceId), period, startDate, endDate);
        return kpiDashboardMetricsQueryService.handle(query)
                .map(AnalyticsTransform::toDashboardResponse)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new DeviceTelemetryUnavailableException(deviceId, false));
    }

    @GetMapping("/devices/{deviceId}/trends")
    @Operation(summary = "Get historical trend chart data for a device")
    @ApiResponse(responseCode = "200", description = "Trend data returned")
    public ResponseEntity<TrendChartResponse> getTrends(
            @Parameter(description = "Device UUID", example = "a1b2c3d4-e5f6-7890-abcd-ef1234567890")
            @PathVariable @NotNull UUID deviceId,
            @RequestParam(required = false) String period,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) @PastOrPresent Instant startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) @PastOrPresent Instant endDate,
            @RequestParam(required = false, defaultValue = "100") Integer limit
    ) {
        if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
            return ResponseEntity.badRequest().build();
        }
        
        TrendPeriod trendPeriod = (period != null) ? TrendPeriod.valueOf(period.toUpperCase()) : null;
        var query = new GetHistoricalTrendQuery(new DeviceId(deviceId), trendPeriod, startDate, endDate, limit);
        var points = kpiHistoricalTrendQueryService.handle(query);
        
        return ResponseEntity.ok(AnalyticsTransform.toTrendChartResponse(points));
    }
}
