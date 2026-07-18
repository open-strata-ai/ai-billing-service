package com.openstrata.billing.application.dto;

/** Budget status view (DESIGN §4 / SPECS §1.2). */
public record BudgetResponse(
    String tenantId,
    long limitCents,
    long spentCents,
    double utilization,
    boolean alerted,
    boolean exceeded
) {}
