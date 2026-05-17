package com.claircore.billing.domain.model.valueobjects;

public enum PlanType {
    FREEMIUM(1, 1, 1),
    PREMIUM(3, 5, 10);

    private final int maxOrganizations;
    private final int maxSpaces;
    private final int maxDevices;

    PlanType(int maxOrganizations, int maxSpaces, int maxDevices) {
        this.maxOrganizations = maxOrganizations;
        this.maxSpaces = maxSpaces;
        this.maxDevices = maxDevices;
    }

    public int maxOrganizations() {
        return maxOrganizations;
    }

    public int maxSpaces() {
        return maxSpaces;
    }

    public int maxDevices() {
        return maxDevices;
    }
}
