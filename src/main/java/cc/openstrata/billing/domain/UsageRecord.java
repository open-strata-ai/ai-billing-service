package cc.openstrata.billing.domain;

import java.time.Instant;

/** A single metering event, idempotent by {@code recordId} (ADR-7). */
public class UsageRecord {

    private final String recordId;
    private final TenantId tenantId;
    private final String appId;
    private final String model;
    private final UsageDimension dimension;
    private final long amount;
    private final Instant occurredAt;

    public UsageRecord(String recordId, TenantId tenantId, String appId, String model,
                       UsageDimension dimension, long amount, Instant occurredAt) {
        this.recordId = recordId;
        this.tenantId = tenantId;
        this.appId = appId;
        this.model = model;
        if (amount < 0) {
            throw new IllegalArgumentException("usage amount must be >= 0");
        }
        this.dimension = dimension;
        this.amount = amount;
        this.occurredAt = occurredAt;
    }

    public String recordId() { return recordId; }
    public TenantId tenantId() { return tenantId; }
    public String appId() { return appId; }
    public String model() { return model; }
    public UsageDimension dimension() { return dimension; }
    public long amount() { return amount; }
    public Instant occurredAt() { return occurredAt; }
}
