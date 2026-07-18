package com.openstrata.billing.domain.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.openstrata.billing.domain.DomainException;
import com.openstrata.billing.web.ErrorCode;
import org.junit.jupiter.api.Test;

class BillingMultitenancyRuleTest {

    @Test
    void requiresMultitenancyWhenDisabled() {
        BillingMultitenancyRule rule = new BillingMultitenancyRule(false);
        DomainException ex = assertThrows(DomainException.class, rule::requireMultitenancy);
        org.junit.jupiter.api.Assertions.assertEquals(ErrorCode.BILLING_REQUIRES_MULTITENANCY, ex.code());
    }

    @Test
    void allowsWhenMultitenancyEnabled() {
        BillingMultitenancyRule rule = new BillingMultitenancyRule(true);
        assertDoesNotThrow(rule::requireMultitenancy);
    }
}
