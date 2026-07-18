package com.openstrata.billing.application;

import com.openstrata.billing.config.OpenstrataProperties;
import com.openstrata.billing.domain.service.BillingMultitenancyRule;
import java.time.LocalDate;
import java.util.List;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * ADR-0003 / DESIGN §4 — AggregationAppService. Scheduled daily settlement. Only runs when
 * multitenancy (billing) is enabled (ARCH §2.2). Pulls per-tenant usage and finalizes the
 * previous period's invoice.
 */
@Service
public class AggregationAppService {

    private final UsageTenantScanner scanner;
    private final InvoiceAppService invoiceAppService;
    private final BillingMultitenancyRule multitenancyRule;

    public AggregationAppService(UsageTenantScanner scanner, InvoiceAppService invoiceAppService,
                                 OpenstrataProperties props) {
        this.scanner = scanner;
        this.invoiceAppService = invoiceAppService;
        this.multitenancyRule = new BillingMultitenancyRule(props.getFeatures().isBillingEnabled());
    }

    /** Default 02:00 daily. Finalize the previous month for every tenant with usage. */
    @Scheduled(cron = "0 0 2 * * *")
    public void aggregateDaily() {
        multitenancyRule.requireMultitenancy();
        String period = LocalDate.now().minusMonths(1).toString().substring(0, 7); // yyyy-MM
        for (String tenantId : scanner.distinctTenants()) {
            invoiceAppService.finalize(tenantId, period, null);
        }
    }

    /** Internal scanner seam (kept small so it can be swapped for a repository call). */
    public interface UsageTenantScanner {
        List<String> distinctTenants();
    }
}
