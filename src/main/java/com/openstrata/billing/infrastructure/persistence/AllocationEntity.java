package com.openstrata.billing.infrastructure.persistence;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/** JPA entity for {@code allocations} (DESIGN §8). amount stored in cents. */
@Entity
@Table(name = "allocations")
public class AllocationEntity {

    @Id
    private String allocId;
    @ManyToOne
    @JoinColumn(name = "invoice_id")
    private InvoiceEntity invoice;
    private String costCenter;
    private long amount;

    public AllocationEntity() {}

    public String getAllocId() { return allocId; }
    public void setAllocId(String allocId) { this.allocId = allocId; }
    public InvoiceEntity getInvoice() { return invoice; }
    public void setInvoice(InvoiceEntity invoice) { this.invoice = invoice; }
    public String getCostCenter() { return costCenter; }
    public void setCostCenter(String costCenter) { this.costCenter = costCenter; }
    public long getAmount() { return amount; }
    public void setAmount(long amount) { this.amount = amount; }
}
