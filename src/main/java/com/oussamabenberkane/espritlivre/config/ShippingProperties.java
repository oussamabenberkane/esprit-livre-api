package com.oussamabenberkane.espritlivre.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for shipping providers (Yalidine, ZR Express).
 */
@ConfigurationProperties(prefix = "application.shipping")
public class ShippingProperties {

    private String originWilaya = "Bejaia";

    private final YalidineConfig yalidine = new YalidineConfig();

    private final ZrExpressConfig zrExpress = new ZrExpressConfig();

    public String getOriginWilaya() {
        return originWilaya;
    }

    public void setOriginWilaya(String originWilaya) {
        this.originWilaya = originWilaya;
    }

    public YalidineConfig getYalidine() {
        return yalidine;
    }

    public ZrExpressConfig getZrExpress() {
        return zrExpress;
    }

    public static class YalidineConfig {

        private String apiId;
        private String apiToken;
        private String webhookSecret;
        private String baseUrl = "https://api.yalidine.app/v1";
        private boolean enabled = false;

        public String getApiId() {
            return apiId;
        }

        public void setApiId(String apiId) {
            this.apiId = apiId;
        }

        public String getApiToken() {
            return apiToken;
        }

        public void setApiToken(String apiToken) {
            this.apiToken = apiToken;
        }

        public String getWebhookSecret() {
            return webhookSecret;
        }

        public void setWebhookSecret(String webhookSecret) {
            this.webhookSecret = webhookSecret;
        }

        public String getBaseUrl() {
            return baseUrl;
        }

        public void setBaseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
        }

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
    }

    public static class ZrExpressConfig {

        private boolean enabled = false;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
    }
}
