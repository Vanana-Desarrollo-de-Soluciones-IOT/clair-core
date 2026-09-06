package com.claircore.evaluation.domain.model.valueobjects;
public record ParticulateMatter(
        Integer pm1_0,
        Integer pm2_5,
        Integer pm10
) {
    public ParticulateMatter {
        if (pm1_0 == null) {
            throw new IllegalArgumentException("pm1_0 must not be null");
        }
        if (pm2_5 == null) {
            throw new IllegalArgumentException("pm2_5 must not be null");
        }
        if (pm10 == null) {
            throw new IllegalArgumentException("pm10 must not be null");
        }
    }
}
