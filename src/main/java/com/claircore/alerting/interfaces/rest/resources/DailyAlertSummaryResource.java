package com.claircore.alerting.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;

@Schema(description = "Daily alert count summary")
public record DailyAlertSummaryResource(
        @Schema(description = "Date", example = "2026-04-27")
        LocalDate date,

        @Schema(description = "Number of alerts on this date", example = "5")
        long count
) {
}
