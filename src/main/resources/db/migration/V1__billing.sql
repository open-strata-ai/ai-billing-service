-- ai-billing-service schema (database: billing).
-- Amounts are stored in integer cents; threshold is a (0,1] ratio (NUMERIC).
-- Flyway owns the schema (spring.jpa.hibernate.ddl-auto=validate in all profiles).
-- Billing entities do not declare @Column(length=...), so Hibernate expects
-- VARCHAR(255) for every string column; lengths below match that exactly.

CREATE TABLE IF NOT EXISTS budgets (
  budget_id   VARCHAR(255) PRIMARY KEY,
  tenant_id   VARCHAR(255) NOT NULL,
  limit_val   BIGINT        NOT NULL,
  spent       BIGINT        NOT NULL,
  threshold   NUMERIC(19,2)
);
CREATE INDEX IF NOT EXISTS idx_budgets_tenant ON budgets(tenant_id);

CREATE TABLE IF NOT EXISTS usage_records (
  record_id   VARCHAR(255) PRIMARY KEY,
  tenant_id   VARCHAR(255) NOT NULL,
  app_id      VARCHAR(255),
  model       VARCHAR(255),
  dimension   VARCHAR(255),
  amount      BIGINT        NOT NULL,
  occurred_at TIMESTAMPTZ
);
CREATE INDEX IF NOT EXISTS idx_usage_tenant ON usage_records(tenant_id);

CREATE TABLE IF NOT EXISTS price_rules (
  rule_id    VARCHAR(255) PRIMARY KEY,
  dimension  VARCHAR(255),
  unit_price BIGINT        NOT NULL,
  currency   VARCHAR(255),
  effective  TIMESTAMPTZ
);

CREATE TABLE IF NOT EXISTS invoices (
  invoice_id   VARCHAR(255) PRIMARY KEY,
  tenant_id    VARCHAR(255) NOT NULL,
  period       VARCHAR(255),
  total        BIGINT        NOT NULL,
  currency     VARCHAR(255),
  status       VARCHAR(255),
  finalized_at TIMESTAMPTZ
);
CREATE INDEX IF NOT EXISTS idx_invoices_tenant ON invoices(tenant_id);

CREATE TABLE IF NOT EXISTS invoice_lines (
  line_id    VARCHAR(255) PRIMARY KEY,
  invoice_id VARCHAR(255) NOT NULL,
  dimension  VARCHAR(255),
  quantity   BIGINT        NOT NULL,
  unit_price BIGINT        NOT NULL,
  subtotal   BIGINT        NOT NULL,
  CONSTRAINT fk_lines_invoice FOREIGN KEY (invoice_id) REFERENCES invoices(invoice_id)
);

CREATE TABLE IF NOT EXISTS allocations (
  alloc_id    VARCHAR(255) PRIMARY KEY,
  invoice_id  VARCHAR(255) NOT NULL,
  cost_center VARCHAR(255),
  amount      BIGINT        NOT NULL,
  CONSTRAINT fk_alloc_invoice FOREIGN KEY (invoice_id) REFERENCES invoices(invoice_id)
);
