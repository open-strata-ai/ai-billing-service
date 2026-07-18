package com.openstrata.billing.domain;

/**
 * Monetary amount stored as a {@code long} in the smallest currency unit (cents for CNY),
 * per ADR-2 / DESIGN §8. NEVER use float/double for money. {@code currency} defaults to CNY.
 */
public record Money(long amount, String currency) {

    public static final String DEFAULT_CURRENCY = "CNY";

    public Money {
        if (amount < 0) {
            throw new IllegalArgumentException("Money amount must be >= 0");
        }
        if (currency == null || currency.isBlank()) {
            currency = DEFAULT_CURRENCY;
        }
    }

    public static Money cents(long amount) {
        return new Money(amount, DEFAULT_CURRENCY);
    }

    public Money add(Money other) {
        if (!this.currency.equals(other.currency)) {
            throw new IllegalArgumentException(
                "Currency mismatch: " + this.currency + " vs " + other.currency);
        }
        return new Money(this.amount + other.amount, this.currency);
    }

    /** Convert cents to yuan for display (e.g. 250 → "2.50"). */
    public String toYuan() {
        return (amount / 100.0) + " " + currency;
    }
}
