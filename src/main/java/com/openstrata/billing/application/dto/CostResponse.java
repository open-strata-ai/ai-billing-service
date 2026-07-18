package com.openstrata.billing.application.dto;

/** Real-time cost profile for a tenant (DESIGN §4 / ARCH §3.1). */
public record CostResponse(
    String tenantId,
    String period,
    long usageCostCents,
    long resourceCostCents,
    long totalCents
) {}
