package cc.openstrata.billing.application;

import cc.openstrata.billing.application.dto.UsageEvent;
import cc.openstrata.billing.config.TenantContext;
import cc.openstrata.billing.domain.TenantId;
import cc.openstrata.billing.domain.UsageRecord;
import cc.openstrata.billing.domain.port.MeteringPort;
import cc.openstrata.billing.infrastructure.persistence.AuditRecorder;
import cc.openstrata.billing.infrastructure.persistence.UsageRecordEntity;
import cc.openstrata.billing.infrastructure.persistence.UsageRecordRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * ADR-0007 — MeteringIngestAppService. Ingests a batch of metering events idempotently
 * (dedup by record_id) and emits {@code UsageAggregated}. Does not self-collect metering
 * (ARCH §2.3 — upstream is ai-gateway-core / Metering Service / OpenCost).
 */
@Service
public class MeteringIngestAppService {

    private final UsageRecordRepository repository;
    private final MeteringPort meteringPort;
    private final AuditRecorder auditRecorder;

    public MeteringIngestAppService(UsageRecordRepository repository,
                                    MeteringPort meteringPort,
                                    AuditRecorder auditRecorder) {
        this.repository = repository;
        this.meteringPort = meteringPort;
        this.auditRecorder = auditRecorder;
    }

    @Transactional
    public int ingest(List<UsageEvent> events) {
        if (events == null || events.isEmpty()) {
            return 0;
        }
        List<UsageRecordEntity> saved = new ArrayList<>();
        for (UsageEvent e : events) {
            if (e.recordId() != null && repository.existsByRecordId(e.recordId())) {
                continue; // idempotent
            }
            Map<String, Object> raw = Map.of(
                "recordId", e.recordId() == null ? "" : e.recordId(),
                "tenantId", e.tenantId(),
                "appId", e.appId() == null ? "" : e.appId(),
                "model", e.model() == null ? "" : e.model(),
                "dimension", e.dimension(),
                "amount", String.valueOf(e.amount()));
            UsageRecord rec = meteringPort.translate(raw);
            UsageRecordEntity entity = new UsageRecordEntity();
            entity.setRecordId(rec.recordId());
            entity.setTenantId(rec.tenantId().value());
            entity.setAppId(rec.appId());
            entity.setModel(rec.model());
            entity.setDimension(rec.dimension().name());
            entity.setAmount(rec.amount());
            entity.setOccurredAt(rec.occurredAt() == null ? Instant.now() : rec.occurredAt());
            saved.add(entity);
        }
        if (!saved.isEmpty()) {
            repository.saveAll(saved);
            auditRecorder.record(actor(), "METERING", tenantOf(events),
                "USAGE_AGGREGATED", Map.of("count", String.valueOf(saved.size())));
        }
        return saved.size();
    }

    private String tenantOf(List<UsageEvent> events) {
        String t = TenantContext.tenantId();
        if (t != null) return t;
        return events.isEmpty() ? "unknown" : events.get(0).tenantId();
    }

    private String actor() {
        String t = TenantContext.tenantId();
        return t == null ? "system" : t;
    }
}
