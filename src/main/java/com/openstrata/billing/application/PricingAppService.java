package com.openstrata.billing.application;

import com.openstrata.billing.domain.TenantId;
import com.openstrata.billing.domain.UsageRecord;
import com.openstrata.billing.domain.service.PricingEngine;
import com.openstrata.billing.infrastructure.persistence.UsageRecordEntity;
import com.openstrata.billing.infrastructure.persistence.UsageRecordRepository;
import com.openstrata.billing.web.ErrorCode;
import java.util.List;
import org.springframework.stereotype.Service;

/** ADR-0001 / DESIGN §5 — PricingAppService. Computes cost from usage × price rules. */
@Service
public class PricingAppService {

    private final UsageRecordRepository usageRecordRepository;
    private final PricingEngine pricingEngine;

    public PricingAppService(UsageRecordRepository usageRecordRepository, PricingEngine pricingEngine) {
        this.usageRecordRepository = usageRecordRepository;
        this.pricingEngine = pricingEngine;
    }

    /** Total cost (cents) for a tenant over the given period prefix (monthly/daily). */
    public long priceTenantPeriod(String tenantId, String period) {
        List<UsageRecord> records = toDomain(usageRecordRepository.findByTenantId(tenantId));
        List<UsageRecord> inPeriod = records.stream()
            .filter(r -> period == null || r.occurredAt().toString().startsWith(period))
            .toList();
        return pricingEngine.priceBatchCents(inPeriod);
    }

    /** Unit price (cents) for a dimension, via the versioned price table (ADR-8). */
    public long findUnitPrice(com.openstrata.billing.domain.UsageDimension dimension) {
        return pricingEngine.findUnitPrice(dimension);
    }

    private List<UsageRecord> toDomain(List<UsageRecordEntity> entities) {
        // Lightweight rehydration for pricing only (tenant/app/model/dimension/amount).
        return entities.stream().map(e -> new UsageRecord(
            e.getRecordId(),
            new TenantId(e.getTenantId()),
            e.getAppId(),
            e.getModel(),
            com.openstrata.billing.domain.UsageDimension.valueOf(e.getDimension()),
            e.getAmount(),
            e.getOccurredAt())).toList();
    }
}
