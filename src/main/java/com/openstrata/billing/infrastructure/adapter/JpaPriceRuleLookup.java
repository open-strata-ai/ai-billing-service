package com.openstrata.billing.infrastructure.adapter;

import com.openstrata.billing.domain.PriceRule;
import com.openstrata.billing.domain.UsageDimension;
import com.openstrata.billing.domain.port.PriceRuleLookup;
import com.openstrata.billing.infrastructure.persistence.PriceRuleEntity;
import com.openstrata.billing.infrastructure.persistence.PriceRuleRepository;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/** JPA-backed {@link PriceRuleLookup} (latest effective rule wins — ADR-8). */
@Component
public class JpaPriceRuleLookup implements PriceRuleLookup {

    private final PriceRuleRepository repository;

    public JpaPriceRuleLookup(PriceRuleRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<PriceRule> findEffective(UsageDimension dimension) {
        return repository.findByDimensionOrderByEffectiveDesc(dimension.name()).stream()
            .map(e -> new PriceRule(e.getRuleId(), dimension, e.getUnitPrice(),
                e.getCurrency(), e.getEffective()))
            .collect(Collectors.toList());
    }
}
