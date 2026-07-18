package cc.openstrata.billing.domain.port;

/** Cache SPI (§4.3.4). Redis default / Valkey optional; tenant-prefixed aggregation cache. */
public interface CachePort {
    void put(String tenantId, String key, Object value);
    Object get(String tenantId, String key);
    void evict(String tenantId, String key);
}
