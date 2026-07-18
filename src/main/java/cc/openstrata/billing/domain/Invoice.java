package cc.openstrata.billing.domain;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Aggregate root: a tenant invoice. Includes {@link InvoiceLine}s and {@link Allocation}s.
 * Immutable after FINALIZED (ADR-6): total must equal the sum of line subtotals and
 * allocations must sum to the total.
 */
public class Invoice {

    private final String invoiceId;
    private final TenantId tenantId;
    private final Period period;
    private final String currency;
    private final List<InvoiceLine> lines = new ArrayList<>();
    private final List<Allocation> allocations = new ArrayList<>();
    private long total; // cents
    private InvoiceStatus status = InvoiceStatus.DRAFT;
    private Instant finalizedAt;

    public Invoice(String invoiceId, TenantId tenantId, Period period, String currency) {
        this.invoiceId = invoiceId;
        this.tenantId = tenantId;
        this.period = period;
        this.currency = currency == null || currency.isBlank() ? Money.DEFAULT_CURRENCY : currency;
    }

    public String invoiceId() { return invoiceId; }
    public TenantId tenantId() { return tenantId; }
    public Period period() { return period; }
    public String currency() { return currency; }
    public List<InvoiceLine> lines() { return List.copyOf(lines); }
    public List<Allocation> allocations() { return List.copyOf(allocations); }
    public long total() { return total; }
    public InvoiceStatus status() { return status; }
    public Instant finalizedAt() { return finalizedAt; }

    public void addLine(InvoiceLine line) {
        if (status.isFinalized()) {
            throw new IllegalStateException("Cannot modify a finalized invoice");
        }
        lines.add(line);
        total += line.subtotal();
    }

    public void addAllocation(Allocation allocation) {
        if (status.isFinalized()) {
            throw new IllegalStateException("Cannot modify a finalized invoice");
        }
        allocations.add(allocation);
    }

    /** Finalize: validate invariants (ADR-6) and lock the invoice. */
    public void finalize() {
        if (status.isFinalized()) {
            throw new DomainException(cc.openstrata.billing.web.ErrorCode.INVOICE_ALREADY_FINALIZED,
                "Invoice already finalized: " + invoiceId);
        }
        long sumLines = lines.stream().mapToLong(InvoiceLine::subtotal).sum();
        if (sumLines != total) {
            throw new IllegalStateException("Invoice total mismatch lines subtotal");
        }
        long sumAlloc = allocations.stream().mapToLong(a -> a.amount().amount()).sum();
        if (!allocations.isEmpty() && sumAlloc != total) {
            throw new IllegalStateException("Allocations must sum to invoice total");
        }
        this.status = InvoiceStatus.FINALIZED;
        this.finalizedAt = Instant.now();
    }
}
