package com.openstrata.billing.domain.port;

import com.openstrata.billing.domain.PriceRule;
import com.openstrata.billing.domain.UsageDimension;
import java.util.List;

/** Read-side port for the configurable, versioned price table (ADR-8). */
public interface PriceRuleLookup {
    List<PriceRule> findEffective(UsageDimension dimension);
}
