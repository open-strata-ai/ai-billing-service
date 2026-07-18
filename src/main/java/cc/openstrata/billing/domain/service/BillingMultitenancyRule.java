package cc.openstrata.billing.domain.service;

import cc.openstrata.billing.web.ErrorCode;

/**
 * ADR-0001/0003/0004 + ARCH §2.2 — BillingMultitenancyRule. Billing is only meaningful in
 * multi-tenant deployments (advanced/full). A single-tenant call must be rejected with
 * 422 BILLING_REQUIRES_MULTITENANCY.
 */
public class BillingMultitenancyRule {

    private final boolean multitenancyEnabled;

    public BillingMultitenancyRule(boolean multitenancyEnabled) {
        this.multitenancyEnabled = multitenancyEnabled;
    }

    public void requireMultitenancy() {
        if (!multitenancyEnabled) {
            throw new cc.openstrata.billing.domain.DomainException(
                ErrorCode.BILLING_REQUIRES_MULTITENANCY,
                "Billing service only enabled in multi-tenant (advanced/full) mode. Enable multitenancy first.");
        }
    }
}
