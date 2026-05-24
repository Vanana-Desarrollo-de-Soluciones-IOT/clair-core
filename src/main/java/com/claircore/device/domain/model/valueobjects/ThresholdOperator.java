package com.claircore.device.domain.model.valueobjects;

public enum ThresholdOperator {
    GREATER_THAN(">"),
    LESS_THAN("<"),
    EQUALS("=="),
    GREATER_THAN_OR_EQUALS(">="),
    LESS_THAN_OR_EQUALS("<=");

    private final String symbol;

    ThresholdOperator(String symbol) {
        this.symbol = symbol;
    }

    public String symbol() { return symbol; }
}