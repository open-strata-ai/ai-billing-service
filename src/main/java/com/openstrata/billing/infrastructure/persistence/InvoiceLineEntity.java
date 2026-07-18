package com.openstrata.billing.infrastructure.persistence;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/** JPA entity for {@code invoice_lines} (DESIGN §8). */
@Entity
@Table(name = "invoice_lines")
public class InvoiceLineEntity {

    @Id
    private String lineId;
    @ManyToOne
    @JoinColumn(name = "invoice_id")
    private InvoiceEntity invoice;
    private String dimension;
    private long quantity;
    private long unitPrice;
    private long subtotal;

    public InvoiceLineEntity() {}

    public String getLineId() { return lineId; }
    public void setLineId(String lineId) { this.lineId = lineId; }
    public InvoiceEntity getInvoice() { return invoice; }
    public void setInvoice(InvoiceEntity invoice) { this.invoice = invoice; }
    public String getDimension() { return dimension; }
    public void setDimension(String dimension) { this.dimension = dimension; }
    public long getQuantity() { return quantity; }
    public void setQuantity(long quantity) { this.quantity = quantity; }
    public long getUnitPrice() { return unitPrice; }
    public void setUnitPrice(long unitPrice) { this.unitPrice = unitPrice; }
    public long getSubtotal() { return subtotal; }
    public void setSubtotal(long subtotal) { this.subtotal = subtotal; }
}
