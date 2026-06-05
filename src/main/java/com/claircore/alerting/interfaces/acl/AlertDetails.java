package com.claircore.alerting.interfaces.acl;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record AlertDetails(
        UUID alertId,
        UUID deviceId,
        UUID spaceId,
        String deviceName,
        String metric,
        BigDecimal thresholdValue,
        BigDecimal actualValue,
        String message,
        String status,
        String severity,
        Instant occurredAt
) {}
