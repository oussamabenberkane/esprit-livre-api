package com.oussamabenberkane.espritlivre.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Properties specific to Esprit Livre.
 * <p>
 * Properties are configured in the {@code application.yml} file.
 * See {@link tech.jhipster.config.JHipsterProperties} for a good example.
 */
@ConfigurationProperties(prefix = "application", ignoreUnknownFields = false)
public class ApplicationProperties {

    private final Liquibase liquibase = new Liquibase();

    private String adminEmail;

    private String devWebsite;

    private String adminPanelUrl;

    private final Keycloak keycloak = new Keycloak();

    private final Meta meta = new Meta();

    private final Bot bot = new Bot();

    // jhipster-needle-application-properties-property

    public Liquibase getLiquibase() {
        return liquibase;
    }

    public String getAdminEmail() {
        return adminEmail;
    }

    public void setAdminEmail(String adminEmail) {
        this.adminEmail = adminEmail;
    }

    public String getDevWebsite() {
        return devWebsite;
    }

    public void setDevWebsite(String devWebsite) {
        this.devWebsite = devWebsite;
    }

    public String getAdminPanelUrl() {
        return adminPanelUrl;
    }

    public void setAdminPanelUrl(String adminPanelUrl) {
        this.adminPanelUrl = adminPanelUrl;
    }

    public Keycloak getKeycloak() {
        return keycloak;
    }

    public Meta getMeta() {
        return meta;
    }

    public Bot getBot() {
        return bot;
    }

    // jhipster-needle-application-properties-property-getter

    public static class Liquibase {

        private Boolean asyncStart = true;

        public Boolean getAsyncStart() {
            return asyncStart;
        }

        public void setAsyncStart(Boolean asyncStart) {
            this.asyncStart = asyncStart;
        }
    }

    public static class Keycloak {

        private String adminUsername;
        private String adminPassword;
        private String adminClientId;

        public String getAdminUsername() {
            return adminUsername;
        }

        public void setAdminUsername(String adminUsername) {
            this.adminUsername = adminUsername;
        }

        public String getAdminPassword() {
            return adminPassword;
        }

        public void setAdminPassword(String adminPassword) {
            this.adminPassword = adminPassword;
        }

        public String getAdminClientId() {
            return adminClientId;
        }

        public void setAdminClientId(String adminClientId) {
            this.adminClientId = adminClientId;
        }
    }
    public static class Meta {

        private boolean enabled = false;
        private String pixelId;
        private String accessToken;
        private String testEventCode;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getPixelId() {
            return pixelId;
        }

        public void setPixelId(String pixelId) {
            this.pixelId = pixelId;
        }

        public String getAccessToken() {
            return accessToken;
        }

        public void setAccessToken(String accessToken) {
            this.accessToken = accessToken;
        }

        public String getTestEventCode() {
            return testEventCode;
        }

        public void setTestEventCode(String testEventCode) {
            this.testEventCode = testEventCode;
        }
    }

    /**
     * WhatsApp/messaging bot integration. The API calls the bot's internal send endpoint
     * over the docker network (never via the public internet) using a shared secret.
     */
    public static class Bot {

        private String internalUrl = "http://whatsapp-agent:8000";
        private String sharedSecret;

        public String getInternalUrl() {
            return internalUrl;
        }

        public void setInternalUrl(String internalUrl) {
            this.internalUrl = internalUrl;
        }

        public String getSharedSecret() {
            return sharedSecret;
        }

        public void setSharedSecret(String sharedSecret) {
            this.sharedSecret = sharedSecret;
        }
    }

    // jhipster-needle-application-properties-property-class
}
