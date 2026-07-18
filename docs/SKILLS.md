# ai-billing-service · AI Coding Rules & Skills (SKILLS)

> **Source**: Extracted from `docs/DESIGN.md` §5 (Domain Rules), §11 (Integration Points), §12 (Security & Multi-tenancy). These rules guide AI-assisted development within this repo.

---

## RULE-01: Multi-tenancy Prerequisite Enforcement

| Aspect | Detail |
| --- | --- |
| **Trigger** | Service startup, any API call, or dependency validation. |
| **Constraint** | `ai-billing-service` MUST NOT be deployed or accept traffic when `multitenancy.enabled=false`. The dependency chain is: `billing` → `multitenancy` → `auth` (§12.4). Violation returns `BILLING_REQUIRES_MULTITENANCY` (HTTP 422). |
| **Rationale** | Billing is meaningless in single-tenant mode; prevents runtime errors from missing multi-tenant infrastructure. |

**Implementation pattern**:
```java
// Domain service: BillingMultitenancyRule
@Component
public class BillingMultitenancyRule {
    public void validate(boolean multitenancyEnabled) {
        if (!multitenancyEnabled) {
            throw new BillingRequiresMultitenancyException(
                "Billing service only enabled in multi-tenant (advanced/full) mode");
        }
    }
}
```

**Test checklist**:
- [ ] Call billing API with `multitenancy=false` → `422 BILLING_REQUIRES_MULTITENANCY`.
- [ ] Service readiness probe fails when `multitenancy` not configured.

---

## RULE-02: Internal Transfer Pricing Computation

| Aspect | Detail |
| --- | --- |
| **Trigger** | Aggregating usage into cost during invoice generation. |
| **Constraint** | Pricing is computed as: `cost = UsageDimension × PriceRule.unitPrice`. Reference rates (§4.7.2): Token ¥2/M input, ¥6/M output; GPU ¥15/h A100; Vector ¥0.5/1M; Document ¥0.1/GB; API ¥0.5/10K. Price table is configurable and versioned. Money is stored in smallest currency unit (cents). |
| **Rationale** | Internal transfer pricing for cost allocation, not external billing. Configurable to support different pricing tiers. |

**Implementation pattern**:
```java
// Domain service: PricingEngine
public Money calculatePrice(UsageRecord record, PriceRule rule) {
    long unitPrice = rule.getUnitPrice();  //in minutes (smallest unit)
    long total = record.getAmount() * unitPrice;
    return new Money(total, rule.getCurrency());
}
```

**Test checklist**:
- [ ] Token input: 1M tokens × ¥2/M = ¥2.
- [ ] GPU: 10 hours × ¥15/h = ¥150.
- [ ] Price rule change → new invoices use updated price; old invoices unchanged.

---

## RULE-03: Budget Guard — Two-Tier Alert & Circuit-Break

| Aspect | Detail |
| --- | --- |
| **Trigger** | `BudgetAppService.check()` called after each aggregation cycle or invoice finalization. |
| **Constraint** | Two thresholds: (1) `spent >= limit × threshold` (default 0.8) → `BudgetAlert` (warning via NotificationPort); (2) `spent >= limit` → `BudgetExceeded` → circuit-break via `QuotaControlPort` to `ai-platform-api`. |
| **Rationale** | Early warning gives tenant admin time to adjust budget; hard circuit-break prevents unbounded cost. |

**Implementation pattern**:
```java
// Domain service: BudgetGuard
public BudgetResult check(Budget budget) {
    long spent = budget.getSpent();
    long limit = budget.getLimitVal();
    double threshold = budget.getThreshold().doubleValue();

    if (spent >= limit) {
        return BudgetResult.exceeded(budget);    // → circuit-break
    } else if (spent >= limit * threshold) {
        return BudgetResult.alert(budget);       // → warning
    }
    return BudgetResult.ok();
}
```

**Test checklist**:
- [ ] Budget 1000, spent 900, threshold 0.8 → `BudgetAlert`.
- [ ] Budget 1000, spent 1000, threshold 0.8 → `BudgetExceeded`.
- [ ] Budget 1000, spent 500 → no alert.

---

## RULE-04: Chargeback/Showback Allocation

| Aspect | Detail |
| --- | --- |
| **Trigger** | Invoice finalization with multi-department/cost-center tenants. |
| **Constraint** | Total invoice cost is allocated by `costCenter` / `department` via `ChargebackRule`. Showback (visibility) vs. Chargeback (internal settlement) are supported (§4.7.2). |
| **Rationale** | Enterprise tenants need to allocate AI costs across business units. |

**Implementation pattern**:
```java
// Domain service: ChargebackRule
public List<Allocation> allocate(Invoice invoice, Map<String, Double> ratios) {
    return ratios.entrySet().stream()
        .map(e -> new Allocation(invoice.getTenantId(), e.getKey(),
            invoice.getTotal().multiply(e.getValue())))
        .toList();
}
```

---

## RULE-05: Metering Aggregation Dimensions

| Aspect | Detail |
| --- | --- |
| **Trigger** | `AggregationAppService.aggregateDaily()` scheduled task. |
| **Constraint** | Aggregate by `tenant × app × model × dimension` tuple. Metering is core (always on, `metering.enabled=true`, §12.1), but **settlement only for multi-tenant**. |
| **Rationale** | Granular aggregation enables cost breakdown by tenant, application, and model for accurate Showback/Chargeback. |

**Test checklist**:
- [ ] Two records for same tenant+app+model+dimension → aggregated into one.
- [ ] Different tenants → separate aggregates.
- [ ] Different dimensions (TOKEN_INPUT vs TOKEN_OUTPUT) → separate aggregates.

---

## RULE-06: Money Precision — Integer Storage

| Aspect | Detail |
| --- | --- |
| **Trigger** | Any price computation or storage of monetary values. |
| **Constraint** | ALL monetary amounts MUST be stored as `BIGINT` in the smallest currency unit (cents for CNY). NEVER use `FLOAT` or `DOUBLE` for money. The `Money` value object enforces this: amount is `long`, currency is `VARCHAR(8)` (default `CNY`). |
| **Rationale** | Avoids floating-point rounding errors; financial industry standard practice. |

**Implementation pattern**:
```java
// Money value object
public record Money(long amount, String currency) {
    public Money {
        if (amount < 0) throw new IllegalArgumentException("Amount must be non-negative");
        if (currency == null || currency.isBlank()) currency = "CNY";
    }
    public Money multiply(double factor) {
        return new Money(Math.round(amount * factor), currency);
    }
}
```

---

## RULE-07: Integration with Metering Sources

| Aspect | Detail |
| --- | --- |
| **Trigger** | Processing metering events from upstream sources. |
| **Constraint** | `MeteringPort` handles events from `ai-gateway-core` (Token/API). `CostSourcePort` handles data from OpenCost (K8s resources). Both sources coexist; aggregation merges by `tenant_id`. Metering events are idempotent (deduplicated by `record_id`). |
| **Rationale** | Complete cost picture requires both LLM usage (gateway) and infrastructure cost (OpenCost). |

**Checklist**:
- [ ] Gateway metering event ingested → `UsageRecord` created.
- [ ] Duplicate `record_id` → idempotent, no double-count.
- [ ] OpenCost metrics → converted to `Money` via `CostSourcePort`.

---

## RULE-08: Tenant Data Isolation (Billing)

| Aspect | Detail |
| --- | --- |
| **Trigger** | Any billing data access (invoice query, budget check, cost query). |
| **Constraint** | All queries STRICTLY filter by `tenant_id`. Cross-tenant cost queries require `platform-admin` role. `tenant_id` column isolation + RLS (§8.2 matrix). Billing service is ONLY deployed in multi-tenant mode; no single-tenant data exists. |
| **Rationale** | Cost data is sensitive; tenant A must never see tenant B's costs. |

**Implementation pattern**:
```java
@PreAuthorize("hasRole('platform-admin') or " +
    "(hasRole('tenant-admin') and #tenantId == authentication.tenantId)")
public List<Invoice> getInvoices(String tenantId) { ... }
```

---

## RULE-09: Audit Trail for Billing Operations

| Aspect | Detail |
| --- | --- |
| **Trigger** | Invoice finalization, budget change, circuit-break action. |
| **Constraint** | All invoicing/budget/circuit-break actions are audited (§14.6 / §4.7.4). Metering ingestion logs are sanitized (no prompt content stored). |
| **Rationale** | Financial operations require full audit trail; prompt content is PII and must not appear in billing logs. |

---

## RULE-10: SPI Contract & Dependency Order

| Aspect | Detail |
| --- | --- |
| **Trigger** | Service startup or dependency upgrade. |
| **Constraint** | Readiness dependency order: PostgreSQL → Keycloak → Redis → Metering sources. Capsule must be available (multi-tenancy prerequisite). SPI versions in `bom.yaml` must be contract-tested. |
| **Rationale** | Billing depends on multi-tenant infrastructure being fully operational. |

**Checklist**:
- [ ] `bom.yaml` versions match deployed infrastructure.
- [ ] `AuthPort` contract test against Keycloak.
- [ ] `CachePort` contract test against Redis + Valkey.
- [ ] `MeteringPort` contract test against gateway/metering service.

---

> **References**: Full domain rules in `docs/DESIGN.md` §5, §11, §12. Cross-reference `ai-platform-api/docs/SKILLS.md` for shared quota/circuit-break rules.
