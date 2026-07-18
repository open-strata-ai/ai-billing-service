package com.openstrata.billing.infrastructure.adapter;

import com.openstrata.billing.domain.TenantId;
import com.openstrata.billing.domain.port.QuotaControlPort;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

/** In-memory QuotaControlPort stand-in for ai-platform-api (R-003 quota circuit breaker). */
@Component
public class InMemoryQuotaControlAdapter implements QuotaControlPort {

    private final Map<String, Boolean> cutoffs = new ConcurrentHashMap<>();
    private final Map<String, Long> spentByTenant = new ConcurrentHashMap<>();

    @Override
    public void cutoff(TenantId tenantId) {
        cutoffs.put(tenantId.value(), Boolean.TRUE);
    }

    @Override
    public void cutoffApp(TenantId tenantId, String appId) {
        cutoffs.put(tenantId.value() + "/" + appId, Boolean.TRUE);
    }

    @Override
    public void reportSpent(TenantId tenantId, long spentCents) {
        spentByTenant.put(tenantId.value(), spentCents);
    }

    public boolean isCutoff(String tenantId) {
        return cutoffs.getOrDefault(tenantId, Boolean.FALSE);
    }
}
