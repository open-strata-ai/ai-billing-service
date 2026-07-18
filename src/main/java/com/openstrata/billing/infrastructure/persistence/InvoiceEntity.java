package com.openstrata.billing.infrastructure.persistence;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/** JPA entity for {@code invoices} (DESIGN §8). total stored in cents. */
@Entity
@Table(name = "invoices")
public class InvoiceEntity {

    @Id
    private String invoiceId;
    private String tenantId;
    private String period;
    private long total;
    private String currency;
    private String status; // InvoiceStatus name
    private Instant finalizedAt;

    public InvoiceEntity() {}

    public String getInvoiceId() { return invoiceId; }
    public void setInvoiceId(String invoiceId) { this.invoiceId = invoiceId; }
    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }
    public String getPeriod() { return period; }
    public void setPeriod(String period) { this.period = period; }
    public long getTotal() { return total; }
    public void setTotal(long total) { this.total = total; }
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Instant getFinalizedAt() { return finalizedAt; }
    public void setFinalizedAt(Instant finalizedAt) { this.finalizedAt = finalizedAt; }
}
