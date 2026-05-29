package com.claircore.analytics.domain.model.valueobjects;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record OverviewDashboardSnapshot(
        OverviewCoreMetrics core,
        List<OrganizationBreakdown> organizations,
        List<AlertSummary> alerts,
        Instant updatedAt
) {
    public OverviewDashboardSnapshot {
        if (core == null) throw new IllegalArgumentException("core must not be null");
        if (organizations == null) throw new IllegalArgumentException("organizations must not be null");
        if (alerts == null) throw new IllegalArgumentException("alerts must not be null");
        if (updatedAt == null) throw new IllegalArgumentException("updatedAt must not be null");
    }

    public record OverviewCoreMetrics(
            Integer aqiValue,
            String aqiCategory,
            Double averageCo2,
            Double averagePm2_5,
            Double averageTemperature,
            Double averageHumidity,
            Double co2DeltaPercentage,
            Double pm2_5DeltaPercentage,
            Double temperatureDeltaPercentage,
            Double humidityDeltaPercentage,
            Instant recordedAt,
            Integer organizationCount,
            Integer spaceCount,
            Integer deviceCount,
            String dataFreshness
    ) {
        public OverviewCoreMetrics {
            if (organizationCount == null || organizationCount < 0) {
                throw new IllegalArgumentException("organizationCount must be >= 0");
            }
            if (spaceCount == null || spaceCount < 0) {
                throw new IllegalArgumentException("spaceCount must be >= 0");
            }
            if (deviceCount == null || deviceCount < 0) {
                throw new IllegalArgumentException("deviceCount must be >= 0");
            }
            if (dataFreshness == null || dataFreshness.isBlank()) {
                throw new IllegalArgumentException("dataFreshness must not be blank");
            }
        }
    }

    public record OrganizationBreakdown(
            UUID organizationId,
            String organizationName,
            List<SpaceBreakdown> spaces
    ) {
        public OrganizationBreakdown {
            if (organizationId == null) throw new IllegalArgumentException("organizationId must not be null");
            if (organizationName == null || organizationName.isBlank()) {
                throw new IllegalArgumentException("organizationName must not be blank");
            }
            if (spaces == null) throw new IllegalArgumentException("spaces must not be null");
        }
    }

    public record SpaceBreakdown(
            UUID spaceId,
            UUID organizationId,
            String spaceName,
            Integer aqiValue,
            String aqiCategory,
            Instant recordedAt,
            Integer deviceCount,
            String dataFreshness
    ) {
        public SpaceBreakdown {
            if (spaceId == null) throw new IllegalArgumentException("spaceId must not be null");
            if (organizationId == null) throw new IllegalArgumentException("organizationId must not be null");
            if (spaceName == null || spaceName.isBlank()) throw new IllegalArgumentException("spaceName must not be blank");
            if (deviceCount == null || deviceCount < 0) throw new IllegalArgumentException("deviceCount must be >= 0");
            if (dataFreshness == null || dataFreshness.isBlank()) throw new IllegalArgumentException("dataFreshness must not be blank");
        }
    }

    public record AlertSummary(
            UUID alertId,
            UUID deviceId,
            UUID spaceId,
            String deviceName,
            String spaceName,
            String message,
            String severity,
            String status,
            Instant occurredAt
    ) {
        public AlertSummary {
            if (alertId == null) throw new IllegalArgumentException("alertId must not be null");
            if (deviceId == null) throw new IllegalArgumentException("deviceId must not be null");
            if (message == null || message.isBlank()) throw new IllegalArgumentException("message must not be blank");
            if (severity == null || severity.isBlank()) throw new IllegalArgumentException("severity must not be blank");
            if (status == null || status.isBlank()) throw new IllegalArgumentException("status must not be blank");
            if (occurredAt == null) throw new IllegalArgumentException("occurredAt must not be null");
        }
    }
}
