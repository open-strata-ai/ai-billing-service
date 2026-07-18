package cc.openstrata.billing.domain.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import cc.openstrata.billing.domain.Allocation;
import cc.openstrata.billing.domain.Invoice;
import cc.openstrata.billing.domain.Money;
import cc.openstrata.billing.domain.Period;
import cc.openstrata.billing.domain.TenantId;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ChargebackRuleTest {

    private Invoice invoice(long totalCents) {
        Invoice inv = new Invoice("inv", new TenantId("t"), new Period("2026-07"), "CNY");
        inv.addLine(new cc.openstrata.billing.domain.InvoiceLine("l", cc.openstrata.billing.domain.UsageDimension.TOKEN_INPUT, 1, totalCents, totalCents));
        return inv;
    }

    @Test
    void splitsByRatioAndSumsToTotal() {
        ChargebackRule rule = new ChargebackRule();
        Invoice inv = invoice(1000);
        List<Allocation> allocs = rule.allocate(inv, Map.of("dept-a", 3.0, "dept-b", 1.0));
        // 75% / 25% → last absorbs remainder
        long a = allocs.stream().filter(x -> "dept-a".equals(x.costCenter())).findFirst().get().amount().amount();
        long b = allocs.stream().filter(x -> "dept-b".equals(x.costCenter())).findFirst().get().amount().amount();
        assertEquals(1000, a + b);
        assertEquals(750, a);
        assertEquals(250, b);
    }

    @Test
    void singleCenterTakesAll() {
        ChargebackRule rule = new ChargebackRule();
        Invoice inv = invoice(500);
        List<Allocation> allocs = rule.allocate(inv, Map.of("only", 1.0));
        assertEquals(1, allocs.size());
        assertEquals(500, allocs.get(0).amount().amount());
    }
}
