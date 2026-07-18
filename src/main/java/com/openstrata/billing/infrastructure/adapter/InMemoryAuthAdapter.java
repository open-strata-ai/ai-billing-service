package com.openstrata.billing.infrastructure.adapter;

import com.openstrata.billing.domain.TenantId;
import com.openstrata.billing.domain.port.AuthPort;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

/** In-memory AuthPort stand-in for Keycloak (auth-contract §3). */
@Component
public class InMemoryAuthAdapter implements AuthPort {

    private final Map<String, Set<String>> roles = new ConcurrentHashMap<>();

    public void grant(String tenantId, String role) {
        roles.computeIfAbsent(tenantId, k -> new HashSet<>()).add(role);
    }

    @Override
    public Set<String> rolesFor(TenantId tenantId) {
        return roles.getOrDefault(tenantId.value(), Set.of());
    }
}
