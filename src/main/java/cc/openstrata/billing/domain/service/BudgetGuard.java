package cc.openstrata.billing.domain.service;

import cc.openstrata.billing.domain.Budget;
import cc.openstrata.billing.domain.TenantId;
import cc.openstrata.billing.domain.port.NotificationPort;
import cc.openstrata.billing.domain.port.QuotaControlPort;
import org.springframework.stereotype.Component;

/**
 * ADR-0005 / R-003 — BudgetGuard. Two-tier check:
 *  - {@code spent >= limit × threshold} → BudgetAlert (warn)
 *  - {@code spent >= limit}           → BudgetExceeded → QuotaControlPort.cutoff(tenantId)
 *    (R-003: tenant-level QPS default; app-level ceiling as backstop, see {@code cutoffApp}).
 */
@Component
public class BudgetGuard {

    private final QuotaControlPort quotaControl;
    private final NotificationPort notification;

    public BudgetGuard(QuotaControlPort quotaControl, NotificationPort notification) {
        this.quotaControl = quotaControl;
        this.notification = notification;
    }

    public enum Level { OK, ALERT, EXCEEDED }

    /** Evaluate a budget; side-effects (alert/circuit-breaker) fire when thresholds are crossed. */
    public Level evaluate(Budget budget) {
        if (budget.exceeded()) {
            quotaControl.cutoff(budget.tenantId());
            notification.alert("Budget exceeded",
                "Tenant " + budget.tenantId().value() + " exceeded budget; quota circuit-breaker tripped");
            return Level.EXCEEDED;
        }
        if (budget.atOrAboveThreshold()) {
            notification.warn("Budget alert",
                "Tenant " + budget.tenantId().value() + " reached "
                    + Math.round(budget.utilization() * 100) + "% of budget");
            return Level.ALERT;
        }
        return Level.OK;
    }

    /** App-level backstop cutoff (R-003 global ceiling). */
    public void cutoffApp(TenantId tenantId, String appId) {
        quotaControl.cutoffApp(tenantId, appId);
        notification.alert("App budget exceeded",
            "Tenant " + tenantId.value() + " app " + appId + " exceeded app-level ceiling");
    }
}
