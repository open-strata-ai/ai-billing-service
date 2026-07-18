package cc.openstrata.billing.application;

import cc.openstrata.billing.application.dto.BudgetResponse;
import cc.openstrata.billing.application.dto.SetBudgetRequest;
import cc.openstrata.billing.domain.AlertThreshold;
import cc.openstrata.billing.domain.Budget;
import cc.openstrata.billing.domain.Money;
import cc.openstrata.billing.domain.TenantId;
import cc.openstrata.billing.domain.service.BudgetGuard;
import cc.openstrata.billing.infrastructure.persistence.BudgetEntity;
import cc.openstrata.billing.infrastructure.persistence.BudgetRepository;
import cc.openstrata.billing.web.ErrorCode;
import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** ADR-0005 — BudgetAppService. Set budget, record spending, run the two-tier guard. */
@Service
public class BudgetAppService {

    private final BudgetRepository budgetRepository;
    private final BudgetGuard budgetGuard;
    private final cc.openstrata.billing.infrastructure.persistence.AuditRecorder auditRecorder;

    public BudgetAppService(BudgetRepository budgetRepository, BudgetGuard budgetGuard,
                            cc.openstrata.billing.infrastructure.persistence.AuditRecorder auditRecorder) {
        this.budgetRepository = budgetRepository;
        this.budgetGuard = budgetGuard;
        this.auditRecorder = auditRecorder;
    }

    @Transactional
    public BudgetResponse setBudget(String tenantId, SetBudgetRequest req) {
        if (req.limitCents() <= 0) {
            throw new cc.openstrata.billing.domain.DomainException(ErrorCode.BAD_REQUEST,
                "budget limit must be > 0");
        }
        AlertThreshold threshold = AlertThreshold.DEFAULT;
        if (req.threshold() > 0) {
            threshold = new AlertThreshold(BigDecimal.valueOf(req.threshold()));
        }
        BudgetEntity entity = budgetRepository.findByTenantId(tenantId)
            .orElseGet(() -> new BudgetEntity());
        boolean isNew = entity.getBudgetId() == null;
        if (isNew) {
            entity.setBudgetId("bud-" + tenantId);
        }
        entity.setTenantId(tenantId);
        entity.setLimitVal(req.limitCents());
        if (isNew) {
            entity.setSpent(0);
        }
        entity.setThreshold(BigDecimal.valueOf(threshold.asDouble()));
        budgetRepository.save(entity);
        auditRecorder.record("system", "BILLING", tenantId, "BUDGET_SET",
            Map.of("limitCents", String.valueOf(req.limitCents())));
        return toResponse(toDomain(entity));
    }

    @Transactional
    public BudgetResponse recordSpending(String tenantId, long spentCents) {
        BudgetEntity entity = budgetRepository.findByTenantId(tenantId)
            .orElseThrow(() -> new cc.openstrata.billing.domain.DomainException(
                ErrorCode.BUDGET_NOT_FOUND, "No budget for tenant " + tenantId));
        entity.setSpent(entity.getSpent() + spentCents);
        budgetRepository.save(entity);
        Budget budget = toDomain(entity);
        budgetGuard.evaluate(budget); // may trip circuit-breaker / alert
        return toResponse(budget);
    }

    public BudgetResponse check(String tenantId) {
        BudgetEntity entity = budgetRepository.findByTenantId(tenantId)
            .orElseThrow(() -> new cc.openstrata.billing.domain.DomainException(
                ErrorCode.BUDGET_NOT_FOUND, "No budget for tenant " + tenantId));
        return toResponse(toDomain(entity));
    }

    private Budget toDomain(BudgetEntity e) {
        return new Budget(e.getBudgetId(), new TenantId(e.getTenantId()),
            Money.cents(e.getLimitVal()), Money.cents(e.getSpent()),
            new AlertThreshold(e.getThreshold()));
    }

    private BudgetResponse toResponse(Budget b) {
        return new BudgetResponse(b.tenantId().value(), b.limit().amount(), b.spent().amount(),
            b.utilization(), b.atOrAboveThreshold(), b.exceeded());
    }
}
