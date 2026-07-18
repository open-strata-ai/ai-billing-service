package cc.openstrata.billing.infrastructure.persistence;

import cc.openstrata.billing.application.AggregationAppService.UsageTenantScanner;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UsageRecordRepository extends JpaRepository<UsageRecordEntity, String>,
    UsageTenantScanner {

    List<UsageRecordEntity> findByTenantId(String tenantId);
    boolean existsByRecordId(String recordId);

    default List<String> distinctTenants() {
        return findAll().stream().map(UsageRecordEntity::getTenantId).distinct().toList();
    }
}
