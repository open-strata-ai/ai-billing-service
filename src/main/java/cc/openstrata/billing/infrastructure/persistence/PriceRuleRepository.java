package cc.openstrata.billing.infrastructure.persistence;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PriceRuleRepository extends JpaRepository<PriceRuleEntity, String> {
    /** Latest effective rule first (ADR-8 versioning). */
    List<PriceRuleEntity> findByDimensionOrderByEffectiveDesc(String dimension);
}
