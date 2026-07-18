package cc.openstrata.billing.application.dto;

/** Request body for {@code PUT /tenants/{tenantId}/budgets}. */
public record SetBudgetRequest(long limitCents, double threshold) {}
