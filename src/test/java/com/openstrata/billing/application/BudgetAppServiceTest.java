package com.openstrata.billing.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.openstrata.billing.application.dto.BudgetResponse;
import com.openstrata.billing.application.dto.SetBudgetRequest;
import com.openstrata.billing.domain.DomainException;
import com.openstrata.billing.domain.service.BudgetGuard;
import com.openstrata.billing.infrastructure.persistence.AuditRecorder;
import com.openstrata.billing.infrastructure.persistence.BudgetEntity;
import com.openstrata.billing.infrastructure.persistence.BudgetRepository;
import com.openstrata.billing.web.ErrorCode;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BudgetAppServiceTest {

    @Mock BudgetRepository budgetRepository;
    @Mock BudgetGuard budgetGuard;
    @Mock AuditRecorder auditRecorder;
    @InjectMocks BudgetAppService service;

    @Test
    void setBudgetPersistsLimit() {
        when(budgetRepository.findByTenantId("t1")).thenReturn(Optional.empty());
        BudgetResponse r = service.setBudget("t1", new SetBudgetRequest(1000, 0.8));
        assertEquals(1000, r.limitCents());
        verify(budgetRepository).save(any());
    }

    @Test
    void setBudgetRejectsNonPositive() {
        assertThrows(DomainException.class, () -> service.setBudget("t1", new SetBudgetRequest(0, 0.8)));
    }

    @Test
    void recordSpendingRunsGuard() {
        BudgetEntity e = new BudgetEntity();
        e.setBudgetId("b-t1");
        e.setTenantId("t1");
        e.setLimitVal(1000);
        e.setSpent(0);
        e.setThreshold(BigDecimal.valueOf(0.8));
        when(budgetRepository.findByTenantId("t1")).thenReturn(Optional.of(e));
        BudgetResponse r = service.recordSpending("t1", 500);
        assertEquals(500, r.spentCents());
        verify(budgetGuard).evaluate(any());
    }

    @Test
    void checkMissingBudgetThrows() {
        when(budgetRepository.findByTenantId("t1")).thenReturn(Optional.empty());
        DomainException ex = assertThrows(DomainException.class, () -> service.check("t1"));
        assertEquals(ErrorCode.BUDGET_NOT_FOUND, ex.code());
    }
}
