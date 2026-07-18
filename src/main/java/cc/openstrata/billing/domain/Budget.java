package cc.openstrata.billing.domain;

/**
 * Aggregate root: one budget per tenant (UNIQUE). Tracks {@code spent} against {@code limit};
 * {@code spent} cannot exceed {@code limit} without the circuit-breaker being tripped (ADR-4/ADR-5).
 */
public class Budget {

    private final String budgetId;
    private final TenantId tenantId;
    private final Money limit;
    private Money spent;
    private final AlertThreshold threshold;

    public Budget(String budgetId, TenantId tenantId, Money limit, Money spent, AlertThreshold threshold) {
        this.budgetId = budgetId;
        this.tenantId = tenantId;
        this.limit = limit;
        this.spent = spent == null ? Money.cents(0) : spent;
        this.threshold = threshold == null ? AlertThreshold.DEFAULT : threshold;
    }

    public String budgetId() { return budgetId; }
    public TenantId tenantId() { return tenantId; }
    public Money limit() { return limit; }
    public Money spent() { return spent; }
    public AlertThreshold threshold() { return threshold; }

    public void recordSpending(Money amount) {
        this.spent = this.spent.add(amount);
    }

    /** Fraction spent vs limit in [0, ∞). */
    public double utilization() {
        if (limit.amount() <= 0) {
            return Double.POSITIVE_INFINITY;
        }
        return (double) spent.amount() / limit.amount();
    }

    public boolean atOrAboveThreshold() {
        return utilization() >= threshold.asDouble();
    }

    public boolean exceeded() {
        return spent.amount() >= limit.amount();
    }
}
