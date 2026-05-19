package com.claircore.evaluation.domain.model.valueobjects;

import jakarta.persistence.Embeddable;

@Embeddable
public record DeviceInfo(
        String chipModel,
        Integer chipRevision,
        Integer cpuFreqMHz,
        Integer flashSize,
        Integer sketchSize,
        Integer freeSketchSpace
) {
    public DeviceInfo {
        if (chipModel == null || chipModel.isBlank()) {
            throw new IllegalArgumentException("chipModel must not be null or blank");
        }
        if (chipRevision == null) {
            throw new IllegalArgumentException("chipRevision must not be null");
        }
        if (cpuFreqMHz == null) {
            throw new IllegalArgumentException("cpuFreqMHz must not be null");
        }
        if (flashSize == null) {
            throw new IllegalArgumentException("flashSize must not be null");
        }
        if (sketchSize == null) {
            throw new IllegalArgumentException("sketchSize must not be null");
        }
        if (freeSketchSpace == null) {
            throw new IllegalArgumentException("freeSketchSpace must not be null");
        }
    }
}
