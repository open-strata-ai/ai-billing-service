package com.openstrata.billing.domain;

/** Billing period: monthly "2026-07" or daily "2026-07-17" (DESIGN §8). */
public record Period(String value) {

    public Period {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("period must not be blank");
        }
    }

    public static Period monthly(int year, int month) {
        return new Period(String.format("%04d-%02d", year, month));
    }

    public static Period daily(int year, int month, int day) {
        return new Period(String.format("%04d-%02d-%02d", year, month, day));
    }
}
