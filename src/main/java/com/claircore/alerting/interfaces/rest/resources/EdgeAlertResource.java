package com.claircore.alerting.interfaces.rest.resources;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;

/**
 * One pending alert as the edge firmware reads it.
 *
 * <p>The snake_case names and the string timestamps are the published contract; this record replaces
 * a hand-built {@code LinkedHashMap} that produced exactly these keys in exactly this order.
 */
public record EdgeAlertResource(
        @JsonProperty("alert_id") String alertId,
        @JsonProperty("device_id") String deviceId,
        @JsonProperty("hardware_id") String hardwareId,
        String metric,
        @JsonProperty("threshold_value") BigDecimal thresholdValue,
        @JsonProperty("actual_value") BigDecimal actualValue,
        String message,
        String status,
        @JsonProperty("occurred_at") String occurredAt,
        @JsonProperty("resolved_at") String resolvedAt
) {}
