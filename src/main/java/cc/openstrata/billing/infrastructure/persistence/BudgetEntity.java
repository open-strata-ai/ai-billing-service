package cc.openstrata.billing.infrastructure.persistence;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;

/** JPA entity for {@code budgets} (DESIGN §8). limit/spent in cents; threshold (0,1]. */
@Entity
@Table(name = "budgets")
public class BudgetEntity {

    @Id
    private String budgetId;
    private String tenantId;
    private long limitVal;
    private long spent;
    private BigDecimal threshold;

    public BudgetEntity() {}

    public String getBudgetId() { return budgetId; }
    public void setBudgetId(String budgetId) { this.budgetId = budgetId; }
    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }
    public long getLimitVal() { return limitVal; }
    public void setLimitVal(long limitVal) { this.limitVal = limitVal; }
    public long getSpent() { return spent; }
    public void setSpent(long spent) { this.spent = spent; }
    public BigDecimal getThreshold() { return threshold; }
    public void setThreshold(BigDecimal threshold) { this.threshold = threshold; }
}
