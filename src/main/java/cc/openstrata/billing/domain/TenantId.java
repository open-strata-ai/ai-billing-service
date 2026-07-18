package cc.openstrata.billing.domain;

/** Multi-tenant isolation key (auth-contract.md §1). */
public record TenantId(String value) {

    public TenantId {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("tenantId must not be blank");
        }
    }
}
