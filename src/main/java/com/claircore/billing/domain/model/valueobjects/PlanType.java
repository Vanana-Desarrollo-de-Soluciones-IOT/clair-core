package com.claircore.billing.domain.model.valueobjects;

public enum PlanType {
    FREEMIUM(1, 1, 1, false),
    PREMIUM(3, 5, 10, true);

    private final int maxOrganizations;
    private final int maxSpaces;
    private final int maxDevices;
    private final boolean monthlyReports;

    PlanType(int maxOrganizations, int maxSpaces, int maxDevices, boolean monthlyReports) {
        this.maxOrganizations = maxOrganizations;
        this.maxSpaces = maxSpaces;
        this.maxDevices = maxDevices;
        this.monthlyReports = monthlyReports;
    }

    public boolean monthlyReports() {
        return monthlyReports;
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
