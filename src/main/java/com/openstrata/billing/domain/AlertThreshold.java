package com.openstrata.billing.domain;

import java.math.BigDecimal;

/** Budget alert threshold ratio in (0.0, 1.0]; default 0.8 (ADR-4). */
public record AlertThreshold(BigDecimal value) {

    public static final AlertThreshold DEFAULT = new AlertThreshold(BigDecimal.valueOf(0.8));

    public AlertThreshold {
        if (value == null || value.compareTo(BigDecimal.ZERO) <= 0
            || value.compareTo(BigDecimal.ONE) > 0) {
            throw new IllegalArgumentException("threshold must be in (0, 1]");
        }
    }

    public BigDecimal asBigDecimal() {
        return value;
    }

    public double asDouble() {
        return value.doubleValue();
    }
}
