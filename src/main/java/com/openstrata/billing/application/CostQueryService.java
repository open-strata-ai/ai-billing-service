package com.openstrata.billing.application;

import com.openstrata.billing.application.dto.CostResponse;
import com.openstrata.billing.domain.port.CostSourcePort;
import com.openstrata.billing.infrastructure.persistence.UsageRecordRepository;
import org.springframework.stereotype.Service;

/** DESIGN §4 / ARCH §3.1 — CostQueryService. Real-time cost profile for a tenant. */
@Service
public class CostQueryService {

    private final PricingAppService pricingAppService;
    private final CostSourcePort costSourcePort;
    private final UsageRecordRepository usageRecordRepository;

    public CostQueryService(PricingAppService pricingAppService, CostSourcePort costSourcePort,
                            UsageRecordRepository usageRecordRepository) {
        this.pricingAppService = pricingAppService;
        this.costSourcePort = costSourcePort;
        this.usageRecordRepository = usageRecordRepository;
    }

    public CostResponse getTenantCost(String tenantId, String period) {
        long usageCost = pricingAppService.priceTenantPeriod(tenantId, period);
        long resourceCost = costSourcePort.pullResourceCost(
            new com.openstrata.billing.domain.TenantId(tenantId), period).amount();
        return new CostResponse(tenantId, period, usageCost, resourceCost, usageCost + resourceCost);
    }
}
