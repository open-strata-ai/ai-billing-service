package cc.openstrata.billing.domain.port;

import cc.openstrata.billing.domain.UsageRecord;
import java.util.List;

/**
 * Metering SPI (§4.7.2). Consumes ai-gateway-core / Metering Service (Go) events and
 * translates external metering records into internal {@link UsageRecord}s.
 */
public interface MeteringPort {
    /** Translate an external metering event into an internal usage record (ACL). */
    UsageRecord translate(Object externalEvent);

    /** Pull recent raw metering events from a source (gateway / metering-service). */
    List<Object> pull(String source);
}
