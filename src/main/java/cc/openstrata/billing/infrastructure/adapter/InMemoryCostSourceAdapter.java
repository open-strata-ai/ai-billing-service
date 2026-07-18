package cc.openstrata.billing.infrastructure.adapter;

import cc.openstrata.billing.domain.Money;
import cc.openstrata.billing.domain.TenantId;
import cc.openstrata.billing.domain.port.CostSourcePort;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

/** In-memory CostSourcePort stand-in for OpenCost (K8s resource cost). */
@Component
public class InMemoryCostSourceAdapter implements CostSourcePort {

    private final Map<String, Money> costs = new ConcurrentHashMap<>();

    public void setCost(String tenantId, Money cost) {
        costs.put(tenantId, cost);
    }

    @Override
    public Money pullResourceCost(TenantId tenantId, String period) {
        return costs.getOrDefault(tenantId.value(), Money.cents(0));
    }
}
