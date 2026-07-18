package com.openstrata.billing.domain.port;

import com.openstrata.billing.domain.TenantId;

/**
 * Quota-control SPI (§8.2). Notifies ai-platform-api to trip a quota circuit breaker when a
 * budget is exceeded (ADR-5 / R-003). This service only signals; platform-api enforces.
 */
public interface QuotaControlPort {
    /** Tenant-level cutoff (R-003 default). */
    void cutoff(TenantId tenantId);

    /** App-level (global) ceiling cutoff (R-003 backstop). */
    void cutoffApp(TenantId tenantId, String appId);

    /** Report updated spent so platform-api can maintain the live quota. */
    void reportSpent(TenantId tenantId, long spentCents);
}
