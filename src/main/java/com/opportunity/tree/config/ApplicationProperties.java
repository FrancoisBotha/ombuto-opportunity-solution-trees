package com.opportunity.tree.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Properties specific to Opportunity Solution Tree.
 * <p>
 * Properties are configured in the {@code application.yml} file.
 * See {@link tech.jhipster.config.JHipsterProperties} for a good example.
 */
@ConfigurationProperties(prefix = "application", ignoreUnknownFields = false)
public class ApplicationProperties {

    private final Liquibase liquibase = new Liquibase();

    private final Seed seed = new Seed();

    // jhipster-needle-application-properties-property

    public Liquibase getLiquibase() {
        return liquibase;
    }

    public Seed getSeed() {
        return seed;
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

    /**
     * Dev seed data ({@code DevDataSeeder}). Off unless {@code application.seed.enabled} is true
     * (set in {@code application-dev.yml}).
     */
    public static class Seed {

        private boolean enabled = false;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
    }

    // jhipster-needle-application-properties-property-class
}
