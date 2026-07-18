package cc.openstrata.billing.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Per auth-contract §3 the gateway (ai-gateway-core) performs JWT validation; this service
 * only enforces the tenant context via {@link AuthInterceptor}. Disable CSRF and let the
 * interceptor own auth so internal service-to-service calls (X-Tenant-Id) work unchanged.
 */
@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
        return http.build();
    }
}
