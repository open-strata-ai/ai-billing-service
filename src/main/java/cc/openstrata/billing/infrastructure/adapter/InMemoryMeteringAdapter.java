package cc.openstrata.billing.infrastructure.adapter;

import cc.openstrata.billing.domain.TenantId;
import cc.openstrata.billing.domain.UsageDimension;
import cc.openstrata.billing.domain.UsageRecord;
import cc.openstrata.billing.domain.port.MeteringPort;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.stereotype.Component;

/**
 * In-memory MeteringPort stand-in for ai-gateway-core / Metering Service (Go).
 * Accepts a simple map-shaped external event and translates it into a {@link UsageRecord}
 * (ACL). Real adapters would consume Kafka/REST batches.
 */
@Component
public class InMemoryMeteringAdapter implements MeteringPort {

    private final AtomicLong seq = new AtomicLong();
    private final Map<String, Object> buffer = new ConcurrentHashMap<>();

    @Override
    @SuppressWarnings("unchecked")
    public UsageRecord translate(Object externalEvent) {
        if (!(externalEvent instanceof Map)) {
            throw new IllegalArgumentException("unsupported metering event");
        }
        Map<String, Object> m = (Map<String, Object>) externalEvent;
        String tenant = String.valueOf(m.getOrDefault("tenantId", "unknown"));
        String appId = String.valueOf(m.getOrDefault("appId", ""));
        String model = String.valueOf(m.getOrDefault("model", ""));
        UsageDimension dim = UsageDimension.valueOf(String.valueOf(m.get("dimension")).toUpperCase());
        long amount = Long.parseLong(String.valueOf(m.getOrDefault("amount", "0")));
        String id = String.valueOf(m.getOrDefault("recordId", "rec-" + seq.incrementAndGet()));
        return new UsageRecord(id, new TenantId(tenant), appId, model, dim, amount, Instant.now());
    }

    @Override
    public List<Object> pull(String source) {
        return List.copyOf(buffer.values().stream().toList());
    }
}
