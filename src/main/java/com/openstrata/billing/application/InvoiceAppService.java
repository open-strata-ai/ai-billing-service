package com.openstrata.billing.application;

import com.openstrata.billing.application.dto.AllocationResponse;
import com.openstrata.billing.application.dto.InvoiceResponse;
import com.openstrata.billing.application.dto.InvoiceLineResponse;
import com.openstrata.billing.domain.Allocation;
import com.openstrata.billing.domain.Invoice;
import com.openstrata.billing.domain.InvoiceLine;
import com.openstrata.billing.domain.Money;
import com.openstrata.billing.domain.Period;
import com.openstrata.billing.domain.TenantId;
import com.openstrata.billing.domain.UsageDimension;
import com.openstrata.billing.domain.UsageRecord;
import com.openstrata.billing.domain.port.QuotaControlPort;
import com.openstrata.billing.infrastructure.persistence.AllocationEntity;
import com.openstrata.billing.infrastructure.persistence.AllocationRepository;
import com.openstrata.billing.infrastructure.persistence.InvoiceEntity;
import com.openstrata.billing.infrastructure.persistence.InvoiceLineEntity;
import com.openstrata.billing.infrastructure.persistence.InvoiceLineRepository;
import com.openstrata.billing.infrastructure.persistence.InvoiceRepository;
import com.openstrata.billing.infrastructure.persistence.UsageRecordEntity;
import com.openstrata.billing.infrastructure.persistence.UsageRecordRepository;
import com.openstrata.billing.web.ErrorCode;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * ADR-0006/0008 — InvoiceAppService. Generates a tenant invoice (monthly/daily) from usage,
 * immutable after FINALIZED. On finalize emits {@code InvoiceFinalized} → QuotaControlPort
 * (report spent) and CostPort (admin dashboard, here: audit log).
 */
@Service
public class InvoiceAppService {

    private final UsageRecordRepository usageRecordRepository;
    private final InvoiceRepository invoiceRepository;
    private final InvoiceLineRepository invoiceLineRepository;
    private final AllocationRepository allocationRepository;
    private final com.openstrata.billing.domain.service.PricingEngine pricingEngine;
    private final QuotaControlPort quotaControl;
    private final com.openstrata.billing.infrastructure.persistence.AuditRecorder auditRecorder;

    public InvoiceAppService(UsageRecordRepository usageRecordRepository,
                             InvoiceRepository invoiceRepository,
                             InvoiceLineRepository invoiceLineRepository,
                             AllocationRepository allocationRepository,
                             com.openstrata.billing.domain.service.PricingEngine pricingEngine,
                             QuotaControlPort quotaControl,
                             com.openstrata.billing.infrastructure.persistence.AuditRecorder auditRecorder) {
        this.usageRecordRepository = usageRecordRepository;
        this.invoiceRepository = invoiceRepository;
        this.invoiceLineRepository = invoiceLineRepository;
        this.allocationRepository = allocationRepository;
        this.pricingEngine = pricingEngine;
        this.quotaControl = quotaControl;
        this.auditRecorder = auditRecorder;
    }

    @Transactional
    public InvoiceResponse finalize(String tenantId, String period, Map<String, Double> costCenters) {
        if (invoiceRepository.findByTenantId(tenantId).stream()
                .anyMatch(i -> period.equals(i.getPeriod()) && "FINALIZED".equals(i.getStatus()))) {
            throw new com.openstrata.billing.domain.DomainException(ErrorCode.INVOICE_ALREADY_FINALIZED,
                "Invoice already finalized for " + tenantId + " period " + period);
        }

        TenantId tid = new TenantId(tenantId);
        Invoice invoice = new Invoice("inv-" + tenantId + "-" + period, tid, new Period(period), Money.DEFAULT_CURRENCY);

        // Aggregate by dimension.
        Map<UsageDimension, Long> byDim = new LinkedHashMap<>();
        List<UsageRecord> records = toDomain(usageRecordRepository.findByTenantId(tenantId));
        for (UsageRecord r : records) {
            if (period != null && !r.occurredAt().toString().startsWith(period)) continue;
            byDim.merge(r.dimension(), r.amount(), Long::sum);
        }

        long lineNo = 0;
        for (Map.Entry<UsageDimension, Long> e : byDim.entrySet()) {
            long unit = pricingEngine.findUnitPrice(e.getKey());
            long subtotal = e.getValue() * unit;
            invoice.addLine(new InvoiceLine("line-" + (++lineNo), e.getKey(), e.getValue(), unit, subtotal));
        }

        if (costCenters != null && !costCenters.isEmpty()) {
            com.openstrata.billing.domain.service.ChargebackRule rule =
                new com.openstrata.billing.domain.service.ChargebackRule();
            List<Allocation> allocs = rule.allocate(invoice, costCenters);
            for (Allocation a : allocs) {
                invoice.addAllocation(a);
            }
        }

        invoice.finalize(); // validates invariants, locks

        InvoiceEntity ie = new InvoiceEntity();
        ie.setInvoiceId(invoice.invoiceId());
        ie.setTenantId(tenantId);
        ie.setPeriod(period);
        ie.setTotal(invoice.total());
        ie.setCurrency(invoice.currency());
        ie.setStatus(invoice.status().name());
        ie.setFinalizedAt(invoice.finalizedAt());
        invoiceRepository.save(ie);

        for (InvoiceLine l : invoice.lines()) {
            InvoiceLineEntity le = new InvoiceLineEntity();
            le.setLineId(l.lineId());
            le.setInvoice(ie);
            le.setDimension(l.dimension().name());
            le.setQuantity(l.quantity());
            le.setUnitPrice(l.unitPrice());
            le.setSubtotal(l.subtotal());
            invoiceLineRepository.save(le);
        }
        for (Allocation a : invoice.allocations()) {
            AllocationEntity ae = new AllocationEntity();
            ae.setAllocId(a.allocationId());
            ae.setInvoice(ie);
            ae.setCostCenter(a.costCenter());
            ae.setAmount(a.amount().amount());
            allocationRepository.save(ae);
        }

        quotaControl.reportSpent(tid, invoice.total());
        auditRecorder.record("system", "BILLING", tenantId, "INVOICE_FINALIZED",
            Map.of("invoiceId", invoice.invoiceId(), "totalCents", String.valueOf(invoice.total())));

        return toResponse(invoice);
    }

    public InvoiceResponse get(String tenantId, String invoiceId) {
        InvoiceEntity ie = invoiceRepository.findByTenantIdAndInvoiceId(tenantId, invoiceId)
            .orElseThrow(() -> new com.openstrata.billing.domain.DomainException(
                ErrorCode.INVOICE_NOT_FOUND, "Invoice not found: " + invoiceId));
        return toResponse(ie);
    }

    public List<InvoiceResponse> list(String tenantId) {
        return invoiceRepository.findByTenantId(tenantId).stream().map(this::toResponse).toList();
    }

    private InvoiceResponse toResponse(Invoice invoice) {
        List<InvoiceLineResponse> lines = invoice.lines().stream().map(l ->
            new InvoiceLineResponse(l.lineId(), l.dimension().name(), l.quantity(), l.unitPrice(), l.subtotal())).toList();
        List<AllocationResponse> allocs = invoice.allocations().stream().map(a ->
            new AllocationResponse(a.allocationId(), a.costCenter(), a.amount().amount())).toList();
        return new InvoiceResponse(invoice.invoiceId(), invoice.tenantId().value(),
            invoice.period().value(), invoice.total(), invoice.currency(), invoice.status().name(), lines, allocs);
    }

    private InvoiceResponse toResponse(InvoiceEntity ie) {
        List<InvoiceLineResponse> lines = invoiceLineRepository.findByInvoiceInvoiceId(ie.getInvoiceId()).stream().map(l ->
            new InvoiceLineResponse(l.getLineId(), l.getDimension(), l.getQuantity(), l.getUnitPrice(), l.getSubtotal())).toList();
        List<AllocationResponse> allocs = allocationRepository.findByInvoiceInvoiceId(ie.getInvoiceId()).stream().map(a ->
            new AllocationResponse(a.getAllocId(), a.getCostCenter(), a.getAmount())).toList();
        return new InvoiceResponse(ie.getInvoiceId(), ie.getTenantId(), ie.getPeriod(), ie.getTotal(),
            ie.getCurrency(), ie.getStatus(), lines, allocs);
    }

    private List<UsageRecord> toDomain(List<UsageRecordEntity> entities) {
        return entities.stream().map(e -> new UsageRecord(
            e.getRecordId(), new TenantId(e.getTenantId()), e.getAppId(), e.getModel(),
            UsageDimension.valueOf(e.getDimension()), e.getAmount(), e.getOccurredAt())).toList();
    }
}
