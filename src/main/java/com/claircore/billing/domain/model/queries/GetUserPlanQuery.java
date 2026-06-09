package com.claircore.billing.domain.model.queries;

public record GetUserPlanQuery(String userId) {
    public GetUserPlanQuery {
        if (userId == null || userId.isBlank()) {
            throw new IllegalArgumentException("userId must not be null or blank");
        }
    }
}
