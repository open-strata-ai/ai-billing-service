package cc.openstrata.billing.infrastructure.adapter;

import cc.openstrata.billing.domain.port.CachePort;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/** In-memory CachePort stand-in for Redis/Valkey (tenant-prefixed). Offline default. */
@Component
@Profile("!prod")
public class InMemoryCacheAdapter implements CachePort {

    private final Map<String, Object> store = new ConcurrentHashMap<>();

    private String key(String tenantId, String key) {
        return tenantId + ":" + key;
    }

    @Override
    public void put(String tenantId, String key, Object value) {
        store.put(key(tenantId, key), value);
    }

    @Override
    public Object get(String tenantId, String key) {
        return store.get(key(tenantId, key));
    }

    @Override
    public void evict(String tenantId, String key) {
        store.remove(key(tenantId, key));
    }
}
