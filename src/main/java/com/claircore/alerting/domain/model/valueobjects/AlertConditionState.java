package com.claircore.alerting.domain.model.valueobjects;

/**
 * Condition state of a metric as reported by Edge/Embedded.
 * This is not the same as an alert lifecycle status (ACTIVE/ACKNOWLEDGED/RESOLVED).
 */
public enum AlertConditionState {
    CRITICAL,
    NORMAL
}
