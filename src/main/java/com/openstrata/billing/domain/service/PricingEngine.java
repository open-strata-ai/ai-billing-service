package com.openstrata.billing.domain.service;

import com.openstrata.billing.domain.Money;
import com.openstrata.billing.domain.UsageDimension;
import com.openstrata.billing.domain.UsageRecord;
import com.openstrata.billing.domain.port.PriceRuleLookup;
import com.openstrata.billing.web.ErrorCode;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * ADR-0001 / DESIGN §5 — PricingEngine. Computes internal transfer price as
 * {@code amount × unitPrice} per UsageDimension, looking the rule up from the
 * versioned price table (ADR-8). Money is BIGINT cents (ADR-2).
 */
@Component
public class PricingEngine {

    private final PriceRuleLookup priceRules;

    public PricingEngine(PriceRuleLookup priceRules) {
        this.priceRules = priceRules;
    }

    /** Unit price (cents) for a dimension, from the latest effective rule (ADR-8). */
    public long findUnitPrice(UsageDimension dimension) {
        List<com.openstrata.billing.domain.PriceRule> rules = priceRules.findEffective(dimension);
        if (rules.isEmpty()) {
            throw new com.openstrata.billing.domain.DomainException(ErrorCode.PRICE_RULE_NOT_FOUND,
                "No price rule for dimension " + dimension);
        }
        // Latest effective rule wins (sorted by effective desc by the repository).
        return rules.get(0).unitPrice();
    }

    /** Cost of a single usage record (amount × unit price, in cents). */
    public long priceCents(UsageRecord record) {
        long unit = findUnitPrice(record.dimension());
        return record.amount() * unit;
    }

    /** Total cost (cents) across a batch of usage records. */
    public long priceBatchCents(List<UsageRecord> records) {
        long total = 0;
        for (UsageRecord r : records) {
            total += priceCents(r);
        }
        return total;
    }

    /** Convenience: wrap a cents total as Money (default currency). */
    public Money toMoney(long cents) {
        return Money.cents(cents);
    }
}
