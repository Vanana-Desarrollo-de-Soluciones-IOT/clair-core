package com.claircore.evaluation.infrastructure.persistence.jpa.embeddables;

import jakarta.persistence.Embeddable;

/** Storage shape of {@code ParticulateMatter}; component names are addressed by the entity. */
@Embeddable
public record ParticulateMatterPersistenceEmbeddable(Integer pm1_0, Integer pm2_5, Integer pm10) {
}
