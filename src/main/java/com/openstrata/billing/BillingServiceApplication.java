package com.openstrata.billing;

import com.openstrata.billing.config.OpenstrataProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

/** ai-billing-service — internal billing/settlement (multi-tenant only, port 8084). */
@SpringBootApplication
@EnableScheduling
@EnableConfigurationProperties(OpenstrataProperties.class)
public class BillingServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(BillingServiceApplication.class, args);
    }
}
