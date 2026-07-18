package cc.openstrata.billing.application;

import cc.openstrata.billing.application.dto.AllocationResponse;
import cc.openstrata.billing.infrastructure.persistence.AllocationEntity;
import cc.openstrata.billing.infrastructure.persistence.AllocationRepository;
import cc.openstrata.billing.web.ErrorCode;
import java.util.List;
import org.springframework.stereotype.Service;

/** ADR-0002 — AllocationAppService. Reads department Showback/Chargeback allocations. */
@Service
public class AllocationAppService {

    private final AllocationRepository allocationRepository;

    public AllocationAppService(AllocationRepository allocationRepository) {
        this.allocationRepository = allocationRepository;
    }

    public List<AllocationResponse> list(String tenantId, String invoiceId) {
        // invoiceId is tenant-scoped; cross-tenant access is blocked by the gateway/AuthInterceptor.
        return allocationRepository.findByInvoiceInvoiceId(invoiceId).stream()
            .map(e -> new AllocationResponse(e.getAllocId(), e.getCostCenter(), e.getAmount()))
            .toList();
    }
}
