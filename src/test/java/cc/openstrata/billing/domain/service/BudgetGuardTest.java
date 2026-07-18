package cc.openstrata.billing.domain.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import cc.openstrata.billing.domain.Budget;
import cc.openstrata.billing.domain.Money;
import cc.openstrata.billing.domain.TenantId;
import cc.openstrata.billing.domain.port.NotificationPort;
import cc.openstrata.billing.domain.port.QuotaControlPort;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class BudgetGuardTest {

    private static final class FakeQuota implements QuotaControlPort {
        final List<String> cutoffs = new ArrayList<>();
        final List<String> appCutoffs = new ArrayList<>();
        public void cutoff(TenantId t) { cutoffs.add(t.value()); }
        public void cutoffApp(TenantId t, String a) { appCutoffs.add(t.value() + "/" + a); }
        public void reportSpent(TenantId t, long s) {}
    }

    private static final class FakeNotify implements NotificationPort {
        final List<String> sent = new ArrayList<>();
        public void alert(String t, String m) { sent.add("A:" + m); }
        public void warn(String t, String m) { sent.add("W:" + m); }
    }

    @Test
    void exceededTripsCircuitBreaker() {
        FakeQuota quota = new FakeQuota();
        FakeNotify notify = new FakeNotify();
        BudgetGuard guard = new BudgetGuard(quota, notify);
        Budget budget = new Budget("b", new TenantId("t"),
            Money.cents(1000), Money.cents(1000), null);
        assertEquals(BudgetGuard.Level.EXCEEDED, guard.evaluate(budget));
        assertEquals(List.of("t"), quota.cutoffs);
        assertEquals(1, notify.sent.size());
    }

    @Test
    void atThresholdRaisesAlert() {
        FakeQuota quota = new FakeQuota();
        FakeNotify notify = new FakeNotify();
        BudgetGuard guard = new BudgetGuard(quota, notify);
        // spent 800, limit 1000, threshold 0.8 → 80% exactly → alert
        Budget budget = new Budget("b", new TenantId("t"),
            Money.cents(1000), Money.cents(800), null);
        assertEquals(BudgetGuard.Level.ALERT, guard.evaluate(budget));
        assertEquals(0, quota.cutoffs.size());
        assertEquals(1, notify.sent.size());
    }

    @Test
    void withinBudgetIsOk() {
        FakeQuota quota = new FakeQuota();
        FakeNotify notify = new FakeNotify();
        BudgetGuard guard = new BudgetGuard(quota, notify);
        Budget budget = new Budget("b", new TenantId("t"),
            Money.cents(1000), Money.cents(500), null);
        assertEquals(BudgetGuard.Level.OK, guard.evaluate(budget));
        assertEquals(0, quota.cutoffs.size());
        assertEquals(0, notify.sent.size());
    }

    @Test
    void appLevelBackstopCutoff() {
        FakeQuota quota = new FakeQuota();
        BudgetGuard guard = new BudgetGuard(quota, new FakeNotify());
        guard.cutoffApp(new TenantId("t"), "app1");
        assertEquals(List.of("t/app1"), quota.appCutoffs);
    }
}
