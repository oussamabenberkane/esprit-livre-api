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
    // jhipster-needle-application-properties-property-class
}
