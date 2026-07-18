package cc.openstrata.billing.domain.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import cc.openstrata.billing.domain.PriceRule;
import cc.openstrata.billing.domain.UsageDimension;
import cc.openstrata.billing.domain.UsageRecord;
import cc.openstrata.billing.domain.port.PriceRuleLookup;
import cc.openstrata.billing.web.ErrorCode;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class PricingEngineTest {

    private PriceRuleLookup lookup(UsageDimension dim, long unitPrice) {
        PriceRule rule = new PriceRule("r1", dim, unitPrice, "CNY", Instant.now());
        return d -> d == dim ? List.of(rule) : List.of();
    }

    @Test
    void pricesSingleRecordByDimension() {
        PricingEngine engine = new PricingEngine(lookup(UsageDimension.TOKEN_INPUT, 2));
        UsageRecord rec = new UsageRecord("x", new cc.openstrata.billing.domain.TenantId("t"),
            "app", "m", UsageDimension.TOKEN_INPUT, 1200, Instant.now());
        assertEquals(2400, engine.priceCents(rec)); // 1200 * 2 (cents)
    }

    @Test
    void throwsWhenNoRule() {
        PricingEngine engine = new PricingEngine(d -> List.of());
        UsageRecord rec = new UsageRecord("x", new cc.openstrata.billing.domain.TenantId("t"),
            "app", "m", UsageDimension.GPU_HOUR, 1, Instant.now());
        var ex = assertThrows(cc.openstrata.billing.domain.DomainException.class,
            () -> engine.priceCents(rec));
        assertEquals(ErrorCode.PRICE_RULE_NOT_FOUND, ex.code());
    }

    @Test
    void batchesMultipleDimensions() {
        PricingEngine engine = new PricingEngine(d -> {
            long u = d == UsageDimension.TOKEN_INPUT ? 2 : 6;
            return List.of(new PriceRule("r", d, u, "CNY", Instant.now()));
        });
        List<UsageRecord> recs = List.of(
            new UsageRecord("a", new cc.openstrata.billing.domain.TenantId("t"), "app", "m",
                UsageDimension.TOKEN_INPUT, 1200, Instant.now()),
            new UsageRecord("b", new cc.openstrata.billing.domain.TenantId("t"), "app", "m",
                UsageDimension.TOKEN_OUTPUT, 350, Instant.now()));
        assertEquals(2400 + 2100, engine.priceBatchCents(recs));
    }
}
