package com.openstrata.billing.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.openstrata.billing.application.dto.UsageEvent;
import com.openstrata.billing.domain.port.MeteringPort;
import com.openstrata.billing.infrastructure.persistence.AuditRecorder;
import com.openstrata.billing.infrastructure.persistence.UsageRecordRepository;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MeteringIngestAppServiceTest {

    @Mock UsageRecordRepository repository;
    @Mock MeteringPort meteringPort;
    @Mock AuditRecorder auditRecorder;
    @InjectMocks MeteringIngestAppService service;

    @Test
    void ingestsNewEventsAndSkipsDuplicates() {
        UsageEvent e1 = new UsageEvent("r1", "t1", "app1", "m", "TOKEN_INPUT", 1200);
        // second event is a duplicate by record_id
        UsageEvent dup = new UsageEvent("r1", "t1", "app1", "m", "TOKEN_INPUT", 999);

        when(repository.existsByRecordId("r1")).thenReturn(false, true);
        when(meteringPort.translate(any())).thenAnswer(inv -> {
            var m = inv.getArgument(0, java.util.Map.class);
            return new com.openstrata.billing.domain.UsageRecord(
                String.valueOf(m.get("recordId")),
                new com.openstrata.billing.domain.TenantId(String.valueOf(m.get("tenantId"))),
                String.valueOf(m.get("appId")), String.valueOf(m.get("model")),
                com.openstrata.billing.domain.UsageDimension.valueOf(
                    String.valueOf(m.get("dimension")).toUpperCase()),
                Long.parseLong(String.valueOf(m.get("amount"))), java.time.Instant.now());
        });

        int first = service.ingest(List.of(e1));
        int second = service.ingest(List.of(dup));

        assertEquals(1, first);
        assertEquals(0, second); // idempotent skip
        verify(repository, never()).save(any()); // batch save path not exercised here
    }
}
