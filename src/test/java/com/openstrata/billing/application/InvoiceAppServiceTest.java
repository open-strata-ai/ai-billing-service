package com.openstrata.billing.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.openstrata.billing.application.dto.InvoiceResponse;
import com.openstrata.billing.domain.UsageDimension;
import com.openstrata.billing.domain.port.QuotaControlPort;
import com.openstrata.billing.domain.service.PricingEngine;
import com.openstrata.billing.infrastructure.persistence.AllocationRepository;
import com.openstrata.billing.infrastructure.persistence.AuditRecorder;
import com.openstrata.billing.infrastructure.persistence.InvoiceLineRepository;
import com.openstrata.billing.infrastructure.persistence.InvoiceRepository;
import com.openstrata.billing.infrastructure.persistence.UsageRecordEntity;
import com.openstrata.billing.infrastructure.persistence.UsageRecordRepository;
import com.openstrata.billing.web.ErrorCode;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class InvoiceAppServiceTest {

    @Mock UsageRecordRepository usageRecordRepository;
    @Mock InvoiceRepository invoiceRepository;
    @Mock InvoiceLineRepository invoiceLineRepository;
    @Mock AllocationRepository allocationRepository;
    @Mock PricingEngine pricingEngine;
    @Mock QuotaControlPort quotaControl;
    @Mock AuditRecorder auditRecorder;
    @InjectMocks InvoiceAppService service;

    private UsageRecordEntity rec(String id, String dim, long amount) {
        UsageRecordEntity e = new UsageRecordEntity();
        e.setRecordId(id);
        e.setTenantId("t1");
        e.setAppId("app1");
        e.setModel("m");
        e.setDimension(dim);
        e.setAmount(amount);
        e.setOccurredAt(Instant.parse("2026-07-15T00:00:00Z"));
        return e;
    }

    @Test
    void finalizeGeneratesInvoiceAndReportsSpent() {
        when(invoiceRepository.findByTenantId("t1")).thenReturn(List.of());
        when(usageRecordRepository.findByTenantId("t1")).thenReturn(List.of(
            rec("r1", UsageDimension.TOKEN_INPUT.name(), 1200),
            rec("r2", UsageDimension.TOKEN_OUTPUT.name(), 350)));
        when(pricingEngine.findUnitPrice(UsageDimension.TOKEN_INPUT)).thenReturn(2L);
        when(pricingEngine.findUnitPrice(UsageDimension.TOKEN_OUTPUT)).thenReturn(6L);

        InvoiceResponse inv = service.finalize("t1", "2026-07", Map.of("dept-a", 1.0));

        assertEquals(1200 * 2 + 350 * 6, inv.totalCents());
        assertEquals(2, inv.lines().size());
        assertEquals(1, inv.allocations().size());
        verify(invoiceRepository).save(any());
        verify(quotaControl).reportSpent(any(), anyLong());
    }

    @Test
    void duplicateFinalizeRejected() {
        com.openstrata.billing.infrastructure.persistence.InvoiceEntity existing =
            new com.openstrata.billing.infrastructure.persistence.InvoiceEntity();
        existing.setInvoiceId("inv-t1-2026-07");
        existing.setTenantId("t1");
        existing.setPeriod("2026-07");
        existing.setStatus("FINALIZED");
        when(invoiceRepository.findByTenantId("t1")).thenReturn(List.of(existing));

        var ex = org.junit.jupiter.api.Assertions.assertThrows(
            com.openstrata.billing.domain.DomainException.class,
            () -> service.finalize("t1", "2026-07", null));
        assertEquals(ErrorCode.INVOICE_ALREADY_FINALIZED, ex.code());
        verify(invoiceRepository, never()).save(any());
    }

    @Test
    void getMissingInvoiceThrows() {
        when(invoiceRepository.findByTenantIdAndInvoiceId("t1", "missing"))
            .thenReturn(java.util.Optional.empty());
        org.junit.jupiter.api.Assertions.assertThrows(
            com.openstrata.billing.domain.DomainException.class,
            () -> service.get("t1", "missing"));
    }
}
