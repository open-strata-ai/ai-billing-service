package cc.openstrata.billing.web;

import cc.openstrata.billing.application.AllocationAppService;
import cc.openstrata.billing.application.BudgetAppService;
import cc.openstrata.billing.application.CostQueryService;
import cc.openstrata.billing.application.InvoiceAppService;
import cc.openstrata.billing.application.MeteringIngestAppService;
import cc.openstrata.billing.application.PricingAppService;
import cc.openstrata.billing.application.dto.AllocationResponse;
import cc.openstrata.billing.application.dto.BudgetResponse;
import cc.openstrata.billing.application.dto.CostResponse;
import cc.openstrata.billing.application.dto.InvoiceResponse;
import cc.openstrata.billing.application.dto.PriceRuleResponse;
import cc.openstrata.billing.application.dto.SetBudgetRequest;
import cc.openstrata.billing.application.dto.UsageEvent;
import cc.openstrata.billing.config.OpenstrataProperties;
import cc.openstrata.billing.domain.UsageDimension;
import cc.openstrata.billing.domain.service.BillingMultitenancyRule;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class BillingController {

    private final OpenstrataProperties props;
    private final MeteringIngestAppService meteringIngest;
    private final InvoiceAppService invoiceAppService;
    private final CostQueryService costQueryService;
    private final BudgetAppService budgetAppService;
    private final AllocationAppService allocationAppService;
    private final PricingAppService pricingAppService;

    public BillingController(OpenstrataProperties props,
                             MeteringIngestAppService meteringIngest,
                             InvoiceAppService invoiceAppService,
                             CostQueryService costQueryService,
                             BudgetAppService budgetAppService,
                             AllocationAppService allocationAppService,
                             PricingAppService pricingAppService) {
        this.props = props;
        this.meteringIngest = meteringIngest;
        this.invoiceAppService = invoiceAppService;
        this.costQueryService = costQueryService;
        this.budgetAppService = budgetAppService;
        this.allocationAppService = allocationAppService;
        this.pricingAppService = pricingAppService;
    }

    /** Enforce the multi-tenancy prerequisite (ARCH §2.2) — read lazily per request. */
    private void requireBilling() {
        new BillingMultitenancyRule(props.getFeatures().isBillingEnabled()).requireMultitenancy();
    }

    @PostMapping("/usage")
    public Map<String, Object> ingest(@RequestBody List<UsageEvent> events) {
        requireBilling();
        int saved = meteringIngest.ingest(events);
        return Map.of("ingested", saved);
    }

    @GetMapping("/tenants/{tenantId}/invoices")
    public List<InvoiceResponse> listInvoices(@PathVariable String tenantId) {
        requireBilling();
        return invoiceAppService.list(tenantId);
    }

    @GetMapping("/tenants/{tenantId}/invoices/{id}")
    public InvoiceResponse getInvoice(@PathVariable String tenantId, @PathVariable String id) {
        requireBilling();
        return invoiceAppService.get(tenantId, id);
    }

    @GetMapping("/tenants/{tenantId}/cost")
    public CostResponse cost(@PathVariable String tenantId,
                             @RequestParam(required = false) String period) {
        requireBilling();
        return costQueryService.getTenantCost(tenantId, period);
    }

    @PutMapping("/tenants/{tenantId}/budgets")
    public BudgetResponse setBudget(@PathVariable String tenantId,
                                   @RequestBody SetBudgetRequest req) {
        requireBilling();
        return budgetAppService.setBudget(tenantId, req);
    }

    @GetMapping("/tenants/{tenantId}/budgets")
    public BudgetResponse getBudget(@PathVariable String tenantId) {
        requireBilling();
        return budgetAppService.check(tenantId);
    }

    @GetMapping("/tenants/{tenantId}/allocations")
    public List<AllocationResponse> allocations(@PathVariable String tenantId,
                                                @RequestParam String invoiceId) {
        requireBilling();
        return allocationAppService.list(tenantId, invoiceId);
    }

    @PostMapping("/reports:generate")
    public Map<String, Object> generateReport(@RequestParam String tenantId,
                                              @RequestParam String period,
                                              @RequestParam(required = false) Map<String, Double> costCenters) {
        requireBilling();
        InvoiceResponse inv = invoiceAppService.finalize(tenantId, period, costCenters);
        return Map.of("invoiceId", inv.invoiceId(), "totalCents", inv.totalCents());
    }

    @GetMapping("/price-rules")
    public List<PriceRuleResponse> priceRules() {
        requireBilling();
        List<PriceRuleResponse> out = new ArrayList<>();
        for (UsageDimension d : UsageDimension.values()) {
            try {
                long unit = pricingAppService.findUnitPrice(d);
                out.add(new PriceRuleResponse(d.name(), unit, "CNY"));
            } catch (RuntimeException ignored) {
                // dimension without a configured rule is omitted
            }
        }
        return out;
    }
}
