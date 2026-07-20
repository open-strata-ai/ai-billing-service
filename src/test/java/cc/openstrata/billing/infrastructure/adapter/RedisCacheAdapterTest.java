package cc.openstrata.billing.infrastructure.adapter;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

class RedisCacheAdapterTest {

    private StringRedisTemplate redis;
    private ValueOperations<String, String> ops;
    private RedisCacheAdapter adapter;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        redis = mock(StringRedisTemplate.class);
        ops = mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(ops);
        adapter = new RedisCacheAdapter(redis, new ObjectMapper());
    }

    @Test
    void putStoresWithTenantPrefix() {
        adapter.put("tenant-1", "balance", 1000);
        verify(ops).set("billing:tenant-1:balance", "1000", Duration.ofMinutes(5));
    }

    @Test
    void putStoresStringValue() {
        adapter.put("t1", "status", "active");
        verify(ops).set("billing:t1:status", "\"active\"", Duration.ofMinutes(5));
    }

    @Test
    void getReturnsRawJson() {
        when(ops.get("billing:t1:data")).thenReturn("{\"amount\":50}");
        Object result = adapter.get("t1", "data");
        assertEquals("{\"amount\":50}", result);
    }

    @Test
    void getReturnsNullWhenMissing() {
        when(ops.get("billing:t1:missing")).thenReturn(null);
        assertNull(adapter.get("t1", "missing"));
    }

    @Test
    void evictDeletesKey() {
        adapter.evict("t1", "temp");
        verify(redis).delete("billing:t1:temp");
    }
}
