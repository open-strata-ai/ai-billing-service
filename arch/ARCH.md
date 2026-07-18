# ai-billing-service · Architecture Decision Document (ARCH)

> **Source**: Extracted from `design/DESIGN.md` §1, §2, §3, §6. Full design doc is the authority; this distillate captures architectural decisions, constraints, and SPI boundaries for implementers.

---

## 1. Service Identity

| Attribute | Value |
| --- | --- |
| Domain | control-plane |
| Language / Framework | Java · Spring Boot 3.x (Jakarta Persistence) |
| Optional | Yes — optional, **only multi-tenant**: enabled in advanced/full profiles |
| Default Port | 8084 |
| Platform Version | v1.4.0 |
| Deployment | 2 replicas (only advanced/full), `ai-system` namespace, 500m CPU / 1Gi request |
| Database | PostgreSQL@16.0 (core base), schema `billing` |
| Prerequisite | `multitenancy.enabled=true` (§12.4: `billing` → `multitenancy` → `auth`) |

---

## 2. Bounded Context

`ai-billing-service` is OpenStrata's **internal billing / settlement service** (Architecture Doc §8.3, §4.7.2). It is effective ONLY for multi-tenant deployments. It aggregates metering data from `ai-gateway-core` and OpenCost by tenant, converts usage into invoices (Showback / Chargeback / cost center), and executes budget alerts and overage circuit-breaking.

### 2.1 Profile-Gated Behavior

| Profile | billing.enabled | Deployed | Description |
| --- | --- | --- | --- |
| starter | false | No | Single-tenant; billing not needed |
| standard | false | No | Single-tenant; billing not needed |
| advanced | true | Yes | Multi-tenant internal settlement |
| full | true | Yes | Full multi-tenant billing + GPU cost tracking |

### 2.2 Dependency Chain

```
billing → multitenancy → auth
```
- If `billing` is lit but `multitenancy` is not: `BILLING_REQUIRES_MULTITENANCY` (HTTP 422)
- Service is not deployed at all for starter/standard profiles (listed in `optional_disabled` in `profiles/*.yaml`)

### 2.3 Upstream Data Sources (Metering — NOT collected by this service)

| Source | Data Type | Collection Mechanism |
| --- | --- | --- |
| `ai-gateway-core` | Token/API metering | Gateway records usage per request; pushes to metering service |
| Metering Service (Go) | Real-time metering | Async push (message/REST batch) to this service |
| OpenCost | K8s resource costs | Pulls CPU/Mem/GPU cost from K8s metrics |

### 2.4 Downstream Consumers

| Consumer | Use | Data |
| --- | --- | --- |
| `ai-admin-service` | Cost dashboard (§14) | Aggregated cost per tenant, department allocation |
| `ai-platform-api` | Quota circuit-breaking | Budget exceeded → quota cut-off signal |
| `ai-portal-frontend` | Invoice display | Tenant invoice list and details |

### 2.5 Boundary Rules
- **Inbound**: Consumes metering events/aggregated results (does NOT self-collect). Authenticated via `Auth` SPI (Keycloak).
- **Out-of-scope (NEVER in this service)**: Token collection, model inference, external charge collection (internal transfer pricing only), prompt content storage.
- **Outbound**: All external calls through SPI Ports. Notifies `ai-platform-api` for quota circuit-breaking.
- **Money Precision**: All monetary amounts stored as `BIGINT` in smallest currency unit (cents for CNY). NEVER `FLOAT` or `DOUBLE`.

---

## 3. Responsibility Matrix

### 3.1 Core Capabilities

| Capability | Description | Arch § | Critical Rules |
| --- | --- | --- | --- |
| Metering Aggregation | Aggregate by `tenant × app × model × dimension`. Token, GPU hours, vector count, API calls, Agent runs. | §4.7.2 | Metering is core (always on), but settlement only for multi-tenant |
| Pricing Engine | Internal transfer pricing: Token ¥2/M input, ¥6/M output, GPU ¥15/h A100, etc. | §4.7.2 / §8.3 | Price table is configurable and versioned; stored in `price_rules` |
| Invoice Generation | Tenant invoices (monthly/daily), department Showback, cost center Chargeback. | §4.7.2 / §8.3 | Invoice immutable after FINALIZED status |
| Budget Alerts | Two-tier: `spent >= limit × threshold` (alert), `spent >= limit` (circuit-break). | §4.7.2 | Alert → WeCom/DingTalk; Exceeded → cut-off via `ai-platform-api` |
| Cost Dashboard | Cost visualization data for admin-service / portal. | §14.4 / §14.5 | Multi-source aggregation (gateway + OpenCost) |
| Quota Linkage | Overage → notify `ai-platform-api` to trigger quota circuit-breaking. | §8.2 / §14.5 | Circuit-break granularity: tenant-level (default), app-level (optional) |

### 3.2 Application Layer Use Cases (from §4)

Aggregation and invoicing use Spring `@Scheduled` / external CronJob. Metering ingestion uses message/REST batching.

| Use Case | App Service | TX Type | Domain Event |
| --- | --- | --- | --- |
| Ingest metering | `MeteringIngestAppService.ingest()` | Write (batch/stream) | `UsageAggregated` |
| Daily aggregation | `AggregationAppService.aggregateDaily()` | Write (scheduled) | `UsageAggregated` |
| Compute cost | `PricingAppService.price()` | Read | — |
| Finalize invoice | `InvoiceAppService.finalize()` | Write | `InvoiceFinalized` |
| Department allocation | `AllocationAppService.allocate()` | Write | — (written into Invoice) |
| Set budget | `BudgetAppService.setBudget()` | Write | — |
| Check budget | `BudgetAppService.check()` | Read | `BudgetAlert` / `BudgetExceeded` |
| Query cost profile | `CostQueryService.getTenantCost()` | Read | — |

### 3.3 Explicit Exclusions

| Responsibility | Handled By |
| --- | --- |
| Token/API metering collection | `ai-gateway-core` (Go) + Metering Service (Go) |
| K8s resource cost collection | OpenCost |
| External payment/collection | Not in scope (internal transfer pricing only) |
| Quota enforcement (circuit-breaking) | `ai-platform-api` (this service only signals) |

---

## 4. Domain Model

### 4.1 Architecture Style

DDD four-layer architecture. Scheduled aggregation (Spring `@Scheduled`, external CronJob). Metering ingestion is idempotent (deduplicated by `record_id`). Money stored as `BIGINT` (smallest currency unit).

### 4.2 Aggregate Design

| Aggregate Root | Consistency Boundary | Key Invariants |
| --- | --- | --- |
| `Invoice` | `UsageRecord` list + `Allocation` list. Immutable after FINALIZED. | Total must equal sum of line subtotals; allocations must sum to total. |
| `Budget` | Standalone, one per tenant (UNIQUE). | `spent` cannot exceed `limit` without triggering circuit-break. |

### 4.3 Entities (Identity-Based)

| Entity | Identity Field | Key Attributes |
| --- | --- | --- |
| `UsageRecord` | `RecordId` | tenantId, appId, model, dimension (enum), amount, occurredAt |
| `Invoice` | `InvoiceId` | tenantId, period, total (BIGINT), currency, status, finalizedAt |
| `InvoiceLine` | `LineId` | invoiceId, dimension, quantity, unitPrice, subtotal |
| `PriceRule` | `PriceRuleId` | dimension, unitPrice (BIGINT, cents), currency, effective date |
| `Allocation` | `AllocationId` | invoiceId, costCenter, amount (BIGINT) |
| `Budget` | `BudgetId` | tenantId (UNIQUE), limit (BIGINT), spent (BIGINT), threshold (NUMERIC) |

### 4.4 Value Objects (Immutable)

| VO | Type | Constraints |
| --- | --- | --- |
| `RecordId`, `InvoiceId`, `PriceRuleId`, `BudgetId`, `AllocationId` | String | UUID format |
| `TenantId` | String | Multi-tenant isolation key |
| `UsageDimension` | Enum | `TOKEN_INPUT`, `TOKEN_OUTPUT`, `GPU_HOUR`, `VECTOR_COUNT`, `API_CALL`, `AGENT_RUN` |
| `Money` | Record(long amount, String currency) | `amount` in smallest currency unit (cents); `currency` default "CNY"; amount >= 0 |
| `Period` | String | `2026-07` (monthly) or `2026-07-17` (daily) |
| `InvoiceStatus` | Enum | `DRAFT` → `FINALIZED` → `SENT` (immutable after FINALIZED) |
| `AlertThreshold` | BigDecimal | Range (0.0, 1.0]; default 0.8 |

### 4.5 Domain Events

| Event | Trigger | Consumer SPI | Side Effects |
| --- | --- | --- | --- |
| `UsageAggregated` | Metering data ingested and aggregated | (internal) | Enters aggregation pipeline for invoicing |
| `InvoiceFinalized` | Invoice generated and finalized | CostPort, QuotaControlPort | Push to admin-service cost dashboard; update ai-platform-api spent |
| `BudgetExceeded` | `spent >= limit` | QuotaControlPort, NotificationPort | Trigger ai-platform-api quota circuit-break; send alert to WeCom/DingTalk |
| `BudgetAlert` | `spent >= limit × threshold` | NotificationPort | Send early warning alert (WeCom/DingTalk) |

### 4.6 Domain Services (Pure Logic)

| Domain Service | Responsibility | Key Rule |
| --- | --- | --- |
| `PricingEngine` | Compute cost: `amount × unitPrice` by dimension | Price table configurable and versioned; unit prices in cents |
| `AggregationRule` | Aggregate usage by `tenant × app × model × dimension` | Metering always on; settlement only multi-tenant |
| `BudgetGuard` | Two-tier budget check: alert threshold, hard limit | Alert at 80% (default); circuit-break at 100% |
| `ChargebackRule` | Allocate invoice total by `costCenter`/`department` ratios | Showback (visibility) + Chargeback (internal settlement) |
| `BillingMultitenancyRule` | Enforce multi-tenancy prerequisite | `tenant_id` mandatory; single-tenant call returns 422 |

---

## 5. SPI Ports & Adapters

### 5.1 Architecture Principle

Domain layer defines Port interfaces only. Infrastructure layer implements Adapters with ACL translation. Multiple metering sources coexist and aggregate by tenant.

### 5.2 Port Inventory

| Port (domain interface) | SPI Port (bom.yaml) | Default Adapter | ACL Responsibility |
| --- | --- | --- | --- |
| `AuthPort` | `Auth` (§4.7.3) | **Keycloak@25.0.0** ✅ | token/claims → `TenantContext` |
| `CachePort` | `Cache` (§4.3.4) | **Redis@7.4.0** ✅ / **Valkey@7.2.0** (optional) | Aggregation intermediate results cache (tenant prefix) |
| `MeteringPort` | — (§4.7.2) | Consume `ai-gateway-core` / Metering Service (Go) events | External metering records ⇄ internal `UsageRecord` |
| `CostSourcePort` | — (§4.7.2) | OpenCost Adapter | External K8s cost data ⇄ internal `Money` |
| `QuotaControlPort` | — (§8.2) | REST → `ai-platform-api` | Invoice/budget status ⇄ quota circuit-breaking DTO |
| `NotificationPort` | — (§4.8 Alerting) | WeCom/DingTalk Adapter (optional) | Alert events ⇄ external messaging |

### 5.3 Aggregation Flow

```
Metering Sources → MeteringPort/CostSourcePort (ACL translation)
  → UsageRecord (idempotent by record_id)
  → Daily CronJob: Aggregate by tenant×app×model×dimension
  → PricingEngine: Compute cost by PriceRule lookup
  → Invoice.finalize() → InvoiceFinalized event
  → CostPort → ai-admin-service (dashboard)
  → QuotaControlPort → ai-platform-api (spent update)
```

### 5.4 Budget Check Flow

```
BudgetGuard.check(budget)
  ├── spent >= limit → BudgetExceeded
  │   ├── QuotaControlPort.cutoff(tenantId) → ai-platform-api
  │   └── NotificationPort.alert("Budget exceeded") → WeCom/DingTalk
  └── spent >= limit × threshold → BudgetAlert
      └── NotificationPort.warn("Budget at {threshold}%") → WeCom/DingTalk
```

### 5.5 Multi-Implementation Strategy

| Scenario | Port | Strategy |
| --- | --- | --- |
| Primary + Alternative (P10) | `CachePort` | Redis (default) + Valkey (optional OSI). Both Spring beans; selected by config. |
| Multi-Source Aggregation | `MeteringPort` + `CostSourcePort` | Gateway metering (Token/API) + OpenCost (K8s). Both sources coexist; aggregated by `tenant_id`. |
| Multi-Channel Notification | `NotificationPort` | WeCom + DingTalk adapters; both can be active simultaneously for alert fan-out. |

### 5.6 External Dependencies (bom.yaml alignment)

| Integration Point | Type | Version | License | Scope | Port |
| --- | --- | --- | --- | --- | --- |
| Keycloak | External OSS | 25.0.0 | Apache-2.0 | core | AuthPort |
| Redis | External OSS | 7.4.0 | BSD-3 | core | CachePort |
| Valkey | External OSS | 7.2.0 | BSD-3 | optional | CachePort |
| PostgreSQL | External OSS | 16.0 | PostgreSQL | core base | — (direct JPA) |
| OpenCost | External OSS | — | Apache-2.0 | core | CostSourcePort |
| ai-gateway-core | Internal (Go) | v1.4.0 | internal | core | MeteringPort |
| Metering Service | Internal (Go) | v1.4.0 | internal | core | MeteringPort |
| ai-platform-api | Internal (Java) | v1.4.0 | internal | core | QuotaControlPort |
| ai-admin-service | Internal (Java) | v1.4.0 | internal | core | CostPort |
| Capsule | External OSS | 1.9.0 | Apache-2.0 | prerequisite | — (indirect, via multitenancy) |

---

## 6. Key Architectural Decisions

| # | Decision | Rationale | Impact |
| --- | --- | --- | --- |
| ADR-1 | Only deployed when `multitenancy.enabled=true` | Starter/standard are single-tenant; billing is meaningless without multi-tenant cost allocation. | Service is not even present in starter/standard profiles (`optional_disabled`). |
| ADR-2 | Money stored as `BIGINT` in smallest currency unit (cents) | Avoids floating-point precision errors in financial computation. Industry standard for billing systems. | All monetary math must use integer arithmetic; display layer converts to yuan. |
| ADR-3 | Async metering ingestion + scheduled aggregation | Decouples real-time metering from batch billing; tolerates metering backpressure without affecting agent execution. | Metering events may have up to 5-min SLA delay before appearing in cost view. |
| ADR-4 | Two-tier budget: alert (80%) + circuit-break (100%) | Early warning gives admin time to adjust budget; hard cut-off prevents unbounded cost. | Budget threshold and limit are configurable per tenant. |
| ADR-5 | Multi-source cost aggregation (gateway + OpenCost) | Token/API costs from gateway; K8s resource costs from OpenCost. Unified by tenant for complete cost picture. | Two independent data sources; need consistent `tenant_id` mapping across both. |
| ADR-6 | Invoice as aggregate root (immutable after FINALIZED) | Ensures financial audit trail integrity; no partial updates to invoiced usage. | Once finalized, invoice cannot be modified; corrections require new invoice generation. |
| ADR-7 | Idempotent metering ingestion (by `record_id`) | Prevents double-counting from retried/deduplicated metering events. | `record_id` must be globally unique; ingestion must handle duplicate key conflicts gracefully. |
| ADR-8 | Pricing table is configurable and versioned | Allows price adjustments per period without affecting historical invoices. | `price_rules.effective` date determines which price applies; must never change for finalized invoices. |

---

## 7. Service Boundary Diagram

```text
┌──────────────────────────────────────────────────────────────┐
│               Upstream Metering Sources                       │
│  ai-gateway-core (Token/API)  │  OpenCost (K8s resources)    │
│  Metering Service (Go, real-time collection)                  │
└────────────────────────────┬─────────────────────────────────┘
                             │ Metering events (async, batch)
┌────────────────────────────▼─────────────────────────────────┐
│               ai-billing-service (8084)                        │
│  (only advanced/full profiles; requires multitenancy)          │
│                                                               │
│  ┌────────────────── Application Layer ──────────────────┐   │
│  │ MeteringIngestAppService (batch/stream)                │   │
│  │ AggregationAppService (scheduled, CronJob)             │   │
│  │ PricingAppService     InvoiceAppService                │   │
│  │ AllocationAppService  BudgetAppService                 │   │
│  │ CostQueryService (CQRS Read)                           │   │
│  └────────────────────────────────────────────────────────┘   │
│  ┌────────────────── Domain Layer (3) ────────────────────┐   │
│  │ Aggregates: Invoice (root), Budget (root)              │   │
│  │ Entities: UsageRecord, PriceRule, Allocation           │   │
│  │ Value Objects: Money (long amount, String currency)    │   │
│  │   UsageDimension, Period, InvoiceStatus, AlertThreshold│   │
│  │ Domain Services: PricingEngine, AggregationRule,       │   │
│  │   BudgetGuard, ChargebackRule, BillingMultitenancyRule │   │
│  │ Domain Events: UsageAggregated, InvoiceFinalized,      │   │
│  │   BudgetExceeded, BudgetAlert                          │   │
│  │                                                         │   │
│  │ Port Interfaces (pure Java, 6 ports):                   │   │
│  │ AuthPort │ CachePort │ MeteringPort │ CostSourcePort   │   │
│  │ QuotaControlPort │ NotificationPort                     │   │
│  └────────────────────────────────────────────────────────┘   │
│  ┌──────────────── Infrastructure Layer (4) ──────────────┐   │
│  │ Adapters:                                               │   │
│  │ KeycloakAdapter │ RedisAdapter/ValkeyAdapter            │   │
│  │ MeteringAdapter (ai-gateway-core)                       │   │
│  │ CostSourceAdapter (OpenCost)                            │   │
│  │ QuotaControlAdapter (ai-platform-api)                   │   │
│  │ NotificationAdapter (WeCom/DingTalk)                    │   │
│  │ JPA: UsageRecordEntity, InvoiceEntity, PriceRuleEntity  │   │
│  │ Flyway: V1__billing_init.sql, V2__rls.sql               │   │
│  └────────────────────────────────────────────────────────┘   │
└────────────────────────────┬─────────────────────────────────┘
                             │ SPI/ACL calls
┌────────────────────────────▼─────────────────────────────────┐
│                  Downstream Consumers                          │
│  ai-admin-service (cost dashboard)                             │
│  ai-platform-api (quota circuit-breaking)                     │
│  ai-portal-frontend (invoice display)                         │
│  WeCom/DingTalk (budget alerts)                               │
└──────────────────────────────────────────────────────────────┘
```

---

> **References**:
> - Full design: `design/DESIGN.md` (16 sections)
> - Architecture framework: `../../OpenStrata architecture design document v2.8.md` §8, §4.7.2, §10.4, §15.5, §16
> - Price engine details: §4.7.2 (internal transfer pricing table)
> - Dependency validation: §12.4 (`billing` → `multitenancy` → `auth`)
