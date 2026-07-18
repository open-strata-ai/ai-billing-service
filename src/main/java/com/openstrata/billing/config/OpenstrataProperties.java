package com.openstrata.billing.config;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** OpenStrata service configuration (DESIGN §10 / SPECS §3). */
@ConfigurationProperties(prefix = "openstrata")
public class OpenstrataProperties {

    private Service service = new Service();
    private Features features = new Features();
    private Pricing pricing = new Pricing();
    private Spi spi = new Spi();

    public static class Service {
        private int port = 8084;
        public int getPort() { return port; }
        public void setPort(int port) { this.port = port; }
    }

    public static class Features {
        /** Billing only enabled when multitenancy is on (ARCH §2.2). */
        private boolean billingEnabled = false;
        private DevMode devMode = new DevMode();
        private BudgetAlert budgetAlert = new BudgetAlert();
        private Showback showback = new Showback();

        public boolean isBillingEnabled() { return billingEnabled; }
        public void setBillingEnabled(boolean v) { this.billingEnabled = v; }
        public DevMode getDevMode() { return devMode; }
        public void setDevMode(DevMode v) { this.devMode = v; }
        public BudgetAlert getBudgetAlert() { return budgetAlert; }
        public void setBudgetAlert(BudgetAlert v) { this.budgetAlert = v; }
        public Showback getShowback() { return showback; }
        public void setShowback(Showback v) { this.showback = v; }

        public static class DevMode {
            private boolean enabled = false;
            public boolean isEnabled() { return enabled; }
            public void setEnabled(boolean v) { this.enabled = v; }
        }

        public static class BudgetAlert {
            private boolean enabled = true;
            private double threshold = 0.8;
            public boolean isEnabled() { return enabled; }
            public void setEnabled(boolean v) { this.enabled = v; }
            public double getThreshold() { return threshold; }
            public void setThreshold(double v) { this.threshold = v; }
        }

        public static class Showback {
            private boolean enabled = true;
            public boolean isEnabled() { return enabled; }
            public void setEnabled(boolean v) { this.enabled = v; }
        }
    }

    public static class Pricing {
        private int tokenInput = 2;
        private int tokenOutput = 6;
        private int gpuHour = 15;
        private double vector1m = 0.5;
        private double docGb = 0.1;
        private double api10k = 0.5;

        public int getTokenInput() { return tokenInput; }
        public void setTokenInput(int v) { this.tokenInput = v; }
        public int getTokenOutput() { return tokenOutput; }
        public void setTokenOutput(int v) { this.tokenOutput = v; }
        public int getGpuHour() { return gpuHour; }
        public void setGpuHour(int v) { this.gpuHour = v; }
        public double getVector1m() { return vector1m; }
        public void setVector1m(double v) { this.vector1m = v; }
        public double getDocGb() { return docGb; }
        public void setDocGb(double v) { this.docGb = v; }
        public double getApi10k() { return api10k; }
        public void setApi10k(double v) { this.api10k = v; }
    }

    public static class Spi {
        private String auth = "keycloak";
        private String cache = "redis";
        private List<String> metering = List.of("ai-gateway-core", "opencost");

        public String getAuth() { return auth; }
        public void setAuth(String v) { this.auth = v; }
        public String getCache() { return cache; }
        public void setCache(String v) { this.cache = v; }
        public List<String> getMetering() { return metering; }
        public void setMetering(List<String> v) { this.metering = v; }
    }

    public Service getService() { return service; }
    public void setService(Service v) { this.service = v; }
    public Features getFeatures() { return features; }
    public void setFeatures(Features v) { this.features = v; }
    public Pricing getPricing() { return pricing; }
    public void setPricing(Pricing v) { this.pricing = v; }
    public Spi getSpi() { return spi; }
    public void setSpi(Spi v) { this.spi = v; }
}
