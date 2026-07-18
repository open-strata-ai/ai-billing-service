package com.openstrata.billing.config;

import com.openstrata.billing.domain.DomainException;
import com.openstrata.billing.web.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashSet;
import java.util.Set;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Populates {@link TenantContext} for every request (auth-contract.md §3).
 *
 * <p>In dev-mode (single-tenant starter, §6) a fixed {@code tenant_id=local} /
 * {@code platform-admin} token is minted and the billing multitenancy gate is bypassed
 * for local runs. Otherwise the tenant is resolved from the {@code X-Tenant-Id} gateway
 * header, or from the {@code tenant_id} / {@code roles} claims of the bearer JWT
 * (claims are base64-decoded only — signature verification is the gateway's job, §2).
 * Requests without an identity are rejected with 401.
 */
@Component
public class AuthInterceptor implements HandlerInterceptor {

    private final OpenstrataProperties props;

    public AuthInterceptor(OpenstrataProperties props) {
        this.props = props;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (props.getFeatures().getDevMode().isEnabled()) {
            TenantContext.set(new TenantContext.Tenant("local", Set.of("platform-admin"), true));
            return true;
        }

        String tenant = request.getHeader("X-Tenant-Id");
        Set<String> roles = new HashSet<>();
        String auth = request.getHeader("Authorization");
        if (StringUtils.hasText(auth) && auth.startsWith("Bearer ")) {
            String token = auth.substring(7).trim();
            Claims claims = decodeClaims(token);
            if (claims != null) {
                if (tenant == null) tenant = claims.tenantId;
                if (claims.roles != null) roles.addAll(claims.roles);
            }
        }
        if (!StringUtils.hasText(tenant)) {
            throw new DomainException(ErrorCode.UNAUTHORIZED,
                "Missing tenant identity (X-Tenant-Id or JWT tenant_id claim)");
        }
        boolean platformAdmin = roles.contains("platform-admin");
        TenantContext.set(new TenantContext.Tenant(tenant, roles, platformAdmin));
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) {
        TenantContext.clear();
    }

    private Claims decodeClaims(String jwt) {
        try {
            String[] parts = jwt.split("\\.");
            if (parts.length < 2) return null;
            String json = new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);
            String tenantId = extract(json, "\"tenant_id\"");
            Set<String> roles = new HashSet<>();
            int idx = json.indexOf("\"roles\"");
            if (idx >= 0) {
                int arrStart = json.indexOf('[', idx);
                int arrEnd = json.indexOf(']', arrStart);
                if (arrStart >= 0 && arrEnd > arrStart) {
                    String arr = json.substring(arrStart + 1, arrEnd);
                    for (String piece : arr.split(",")) {
                        String r = extract(piece, "\"");
                        if (r != null) roles.add(r);
                    }
                }
            }
            return new Claims(tenantId, roles);
        } catch (RuntimeException e) {
            return null;
        }
    }

    private String extract(String json, String key) {
        int i = json.indexOf(key);
        if (i < 0) return null;
        int colon = json.indexOf(':', i);
        if (colon < 0) return null;
        int q1 = json.indexOf('"', colon);
        int q2 = json.indexOf('"', q1 + 1);
        if (q1 < 0 || q2 < 0) return null;
        return json.substring(q1 + 1, q2);
    }

    private record Claims(String tenantId, Set<String> roles) {}
}
