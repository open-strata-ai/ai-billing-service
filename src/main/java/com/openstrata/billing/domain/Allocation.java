package com.openstrata.billing.domain;

/** A department/cost-center Shareback or Chargeback line of an {@link Invoice} (DESIGN §3/§5). */
public class Allocation {

    private final String allocationId;
    private final String costCenter;
    private final Money amount;

    public Allocation(String allocationId, String costCenter, Money amount) {
        this.allocationId = allocationId;
        this.costCenter = costCenter;
        this.amount = amount;
    }

    public String allocationId() { return allocationId; }
    public String costCenter() { return costCenter; }
    public Money amount() { return amount; }
}
