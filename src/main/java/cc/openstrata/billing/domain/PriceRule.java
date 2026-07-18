package cc.openstrata.billing.domain;

import java.time.Instant;

/** A configurable, versioned price (ADR-8). Unit price is in cents (Money amount). */
public class PriceRule {

    private final String ruleId;
    private final UsageDimension dimension;
    private final long unitPrice; // cents per unit (see DESIGN §5 example table)
    private final String currency;
    private final Instant effective;

    public PriceRule(String ruleId, UsageDimension dimension, long unitPrice,
                     String currency, Instant effective) {
        this.ruleId = ruleId;
        this.dimension = dimension;
        this.unitPrice = unitPrice;
        this.currency = currency == null || currency.isBlank() ? Money.DEFAULT_CURRENCY : currency;
        this.effective = effective;
    }

    public String ruleId() { return ruleId; }
    public UsageDimension dimension() { return dimension; }
    public long unitPrice() { return unitPrice; }
    public String currency() { return currency; }
    public Instant effective() { return effective; }

    public Money unitPriceAsMoney() {
        return new Money(unitPrice, currency);
    }
}
