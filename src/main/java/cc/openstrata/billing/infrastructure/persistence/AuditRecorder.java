package cc.openstrata.billing.infrastructure.persistence;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import org.springframework.stereotype.Component;

/** In-memory append-only audit recorder (DESIGN §12 audit of billing/budget actions). */
@Component
public class AuditRecorder {

    public record AuditEntry(String actor, String scope, String tenantId,
                             String action, Instant at, Map<String, String> details) {}

    private final List<AuditEntry> entries = new CopyOnWriteArrayList<>();

    public AuditEntry record(String actor, String scope, String tenantId,
                             String action, Map<String, String> details) {
        AuditEntry e = new AuditEntry(actor, scope, tenantId, action, Instant.now(), details);
        entries.add(e);
        return e;
    }

    public List<AuditEntry> all() {
        return List.copyOf(entries);
    }
}
