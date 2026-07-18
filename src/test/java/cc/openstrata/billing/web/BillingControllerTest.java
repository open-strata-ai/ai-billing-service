package cc.openstrata.billing.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import cc.openstrata.billing.application.AllocationAppService;
import cc.openstrata.billing.application.BudgetAppService;
import cc.openstrata.billing.application.CostQueryService;
import cc.openstrata.billing.application.InvoiceAppService;
import cc.openstrata.billing.application.MeteringIngestAppService;
import cc.openstrata.billing.application.PricingAppService;
import cc.openstrata.billing.application.dto.BudgetResponse;
import cc.openstrata.billing.application.dto.InvoiceResponse;
import cc.openstrata.billing.config.OpenstrataProperties;
import cc.openstrata.billing.domain.port.QuotaControlPort;
import cc.openstrata.billing.domain.service.PricingEngine;
import cc.openstrata.billing.infrastructure.persistence.AuditRecorder;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(BillingController.class)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
class BillingControllerTest {

    @Autowired MockMvc mvc;

    @MockBean OpenstrataProperties props;
    @MockBean MeteringIngestAppService meteringIngest;
    @MockBean InvoiceAppService invoiceAppService;
    @MockBean CostQueryService costQueryService;
    @MockBean BudgetAppService budgetAppService;
    @MockBean AllocationAppService allocationAppService;
    @MockBean PricingAppService pricingAppService;
    // InvoiceAppService constructor also needs these to satisfy context wiring.
    @MockBean PricingEngine pricingEngine;
    @MockBean QuotaControlPort quotaControl;
    @MockBean AuditRecorder auditRecorder;

    @Test
    void singleTenantCallReturns422() throws Exception {
        OpenstrataProperties.Features f = new OpenstrataProperties.Features();
        f.setBillingEnabled(false);
        f.getDevMode().setEnabled(true);
        when(props.getFeatures()).thenReturn(f);

        mvc.perform(get("/api/v1/tenants/t1/invoices"))
            .andExpect(status().is(422))
            .andExpect(jsonPath("$.code").value("BILLING_REQUIRES_MULTITENANCY"));
    }

    @Test
    void listsInvoicesWhenEnabled() throws Exception {
        OpenstrataProperties.Features f = new OpenstrataProperties.Features();
        f.setBillingEnabled(true);
        f.getDevMode().setEnabled(true);
        when(props.getFeatures()).thenReturn(f);
        when(invoiceAppService.list("t1")).thenReturn(List.of(
            new InvoiceResponse("inv-1", "t1", "2026-07", 100, "CNY", "FINALIZED", List.of(), List.of())));

        mvc.perform(get("/api/v1/tenants/t1/invoices"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].invoiceId").value("inv-1"));
    }

    @Test
    void setsBudget() throws Exception {
        OpenstrataProperties.Features f = new OpenstrataProperties.Features();
        f.setBillingEnabled(true);
        f.getDevMode().setEnabled(true);
        when(props.getFeatures()).thenReturn(f);
        when(budgetAppService.setBudget(eq("t1"), any())).thenReturn(
            new BudgetResponse("t1", 1000, 0, 0.0, false, false));

        mvc.perform(put("/api/v1/tenants/t1/budgets")
                .contentType(MediaType.APPLICATION_JSON).content("{\"limitCents\":1000,\"threshold\":0.8}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.limitCents").value(1000));
    }

    @Test
    void ingestsUsage() throws Exception {
        OpenstrataProperties.Features f = new OpenstrataProperties.Features();
        f.setBillingEnabled(true);
        f.getDevMode().setEnabled(true);
        when(props.getFeatures()).thenReturn(f);
        when(meteringIngest.ingest(any())).thenReturn(2);

        mvc.perform(post("/api/v1/usage")
                .contentType(MediaType.APPLICATION_JSON)
                .content("[{\"recordId\":\"r1\",\"tenantId\":\"t1\",\"dimension\":\"TOKEN_INPUT\",\"amount\":10}]"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.ingested").value(2));
    }
}
