package cc.openstrata.billing.application.dto;

import java.util.List;

/** Read-side view of an invoice + lines + allocations. */
public record InvoiceResponse(
    String invoiceId,
    String tenantId,
    String period,
    long totalCents,
    String currency,
    String status,
    List<InvoiceLineResponse> lines,
    List<AllocationResponse> allocations
) {}
