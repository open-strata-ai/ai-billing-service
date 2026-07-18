package com.openstrata.billing.domain.service;

import com.openstrata.billing.domain.Allocation;
import com.openstrata.billing.domain.Invoice;
import com.openstrata.billing.domain.Money;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * ADR-0002 / DESIGN §5 — ChargebackRule. Apportions an invoice total across
 * cost centers / departments by ratio (Showback = visibility, Chargeback = internal
 * settlement). Ratios must sum to ~1.0; the remainder (rounding) lands on the first center.
 */
public class ChargebackRule {

    /**
     * @param invoice the invoice to allocate (total must be set)
     * @param shares  costCenter → weight (any positive scale; normalized internally)
     * @return allocations whose amounts sum to the invoice total
     */
    public List<Allocation> allocate(Invoice invoice, Map<String, Double> shares) {
        if (shares == null || shares.isEmpty()) {
            throw new IllegalArgumentException("chargeback shares must not be empty");
        }
        long total = invoice.total();
        double sum = shares.values().stream().mapToDouble(Double::doubleValue).sum();
        if (sum <= 0) {
            throw new IllegalArgumentException("chargeback share sum must be > 0");
        }
        List<Allocation> result = new ArrayList<>();
        long allocated = 0;
        int i = 0;
        int n = shares.size();
        for (Map.Entry<String, Double> e : shares.entrySet()) {
            i++;
            long amount;
            if (i == n) {
                // last center absorbs rounding remainder so allocations sum exactly to total
                amount = total - allocated;
            } else {
                amount = Math.round(total * (e.getValue() / sum));
                allocated += amount;
            }
            result.add(new Allocation(invoice.invoiceId() + "-" + e.getKey(),
                e.getKey(), Money.cents(Math.max(0, amount))));
        }
        return result;
    }
}
