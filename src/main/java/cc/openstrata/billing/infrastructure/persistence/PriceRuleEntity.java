package cc.openstrata.billing.infrastructure.persistence;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/** JPA entity for {@code price_rules} (DESIGN §8). unitPrice stored in cents. */
@Entity
@Table(name = "price_rules")
public class PriceRuleEntity {

    @Id
    private String ruleId;
    private String dimension;
    private long unitPrice;
    private String currency;
    private Instant effective;

    public PriceRuleEntity() {}

    public String getRuleId() { return ruleId; }
    public void setRuleId(String ruleId) { this.ruleId = ruleId; }
    public String getDimension() { return dimension; }
    public void setDimension(String dimension) { this.dimension = dimension; }
    public long getUnitPrice() { return unitPrice; }
    public void setUnitPrice(long unitPrice) { this.unitPrice = unitPrice; }
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    public Instant getEffective() { return effective; }
    public void setEffective(Instant effective) { this.effective = effective; }
}
