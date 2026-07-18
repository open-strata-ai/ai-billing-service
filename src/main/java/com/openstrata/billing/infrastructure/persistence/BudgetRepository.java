package com.openstrata.billing.infrastructure.persistence;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BudgetRepository extends JpaRepository<BudgetEntity, String> {
    Optional<BudgetEntity> findByTenantId(String tenantId);
}
