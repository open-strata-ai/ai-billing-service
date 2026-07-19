package cc.openstrata.billing.infrastructure.adapter;

import cc.openstrata.billing.domain.port.CachePort;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/** Redis-backed CachePort for prod profile (SPRING_PROFILES_ACTIVE=prod). */
@Component
@Profile("prod")
public class RedisCacheAdapter implements CachePort {

    private final StringRedisTemplate redis;
    private final ObjectMapper mapper;
    private static final Duration TTL = Duration.ofMinutes(5);

    public RedisCacheAdapter(StringRedisTemplate redis, ObjectMapper mapper) {
        this.redis = redis;
        this.mapper = mapper;
    }

    @Override
    public void put(String tenantId, String key, Object value) {
        try {
            redis.opsForValue().set(key(tenantId, key), mapper.writeValueAsString(value), TTL);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize cache value for key: " + key, e);
        }
    }

    @Override
    public Object get(String tenantId, String key) {
        String raw = redis.opsForValue().get(key(tenantId, key));
        if (raw == null) return null;
        // Return raw JSON string; callers deserialize as needed
        return raw;
    }

    @Override
    public void evict(String tenantId, String key) {
        redis.delete(key(tenantId, key));
    }

    private static String key(String tenantId, String k) {
        return "billing:" + tenantId + ":" + k;
    }
}
