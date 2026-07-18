package cc.openstrata.billing.domain.port;

import cc.openstrata.billing.domain.TenantId;
import java.util.Set;

/** Auth SPI (§4.7.3). Keycloak adapter is the default; resolves tenant/roles from token. */
public interface AuthPort {
    Set<String> rolesFor(TenantId tenantId);
}
