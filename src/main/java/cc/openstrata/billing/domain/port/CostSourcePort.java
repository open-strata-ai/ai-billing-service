package cc.openstrata.billing.domain.port;

import cc.openstrata.billing.domain.Money;
import cc.openstrata.billing.domain.TenantId;

/** Cost SPI (§4.7.2). OpenCost adapter translates K8s resource cost into internal Money. */
public interface CostSourcePort {
    /** Pull K8s resource cost for a tenant over a period (ADR-5 multi-source). */
    Money pullResourceCost(TenantId tenantId, String period);
}
