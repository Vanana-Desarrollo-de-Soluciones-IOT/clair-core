package com.claircore.evaluation.domain.model.valueobjects;

import jakarta.persistence.Embeddable;

@Embeddable
public record DeviceHealth(
        Integer freeHeap,
        Integer minFreeHeap,
        Integer heapSize,
        Integer maxAllocHeap,
        String scd41Status,
        String pms5003Status,
        Integer lastValidAirQualitySec,
        Integer lastValidPMSec
) {
    public DeviceHealth {
        if (freeHeap == null) {
            throw new IllegalArgumentException("freeHeap must not be null");
        }
        if (minFreeHeap == null) {
            throw new IllegalArgumentException("minFreeHeap must not be null");
        }
        if (heapSize == null) {
            throw new IllegalArgumentException("heapSize must not be null");
        }
        if (maxAllocHeap == null) {
            throw new IllegalArgumentException("maxAllocHeap must not be null");
        }
        if (scd41Status == null || scd41Status.isBlank()) {
            throw new IllegalArgumentException("scd41Status must not be null or blank");
        }
        if (pms5003Status == null || pms5003Status.isBlank()) {
            throw new IllegalArgumentException("pms5003Status must not be null or blank");
        }
        if (lastValidAirQualitySec == null) {
            throw new IllegalArgumentException("lastValidAirQualitySec must not be null");
        }
        if (lastValidPMSec == null) {
            throw new IllegalArgumentException("lastValidPMSec must not be null");
        }
    }
}
