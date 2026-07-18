package com.openstrata.billing.infrastructure.persistence;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/** JPA entity for {@code usage_records} (DESIGN §8). */
@Entity
@Table(name = "usage_records")
public class UsageRecordEntity {

    @Id
    private String recordId;
    private String tenantId;
    private String appId;
    private String model;
    private String dimension; // UsageDimension enum name
    private long amount;
    private Instant occurredAt;

    public UsageRecordEntity() {}

    public String getRecordId() { return recordId; }
    public void setRecordId(String recordId) { this.recordId = recordId; }
    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }
    public String getAppId() { return appId; }
    public void setAppId(String appId) { this.appId = appId; }
    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }
    public String getDimension() { return dimension; }
    public void setDimension(String dimension) { this.dimension = dimension; }
    public long getAmount() { return amount; }
    public void setAmount(long amount) { this.amount = amount; }
    public Instant getOccurredAt() { return occurredAt; }
    public void setOccurredAt(Instant occurredAt) { this.occurredAt = occurredAt; }
}
