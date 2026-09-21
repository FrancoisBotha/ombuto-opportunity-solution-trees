package com.opportunity.tree.config;

import java.util.ArrayList;
import java.util.List;
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

    private final Mcp mcp = new Mcp();

    private final DefaultLinks defaultLinks = new DefaultLinks();

    // jhipster-needle-application-properties-property

    public Liquibase getLiquibase() {
        return liquibase;
    }

    public Seed getSeed() {
        return seed;
    }

    public Mcp getMcp() {
        return mcp;
    }

    public DefaultLinks getDefaultLinks() {
        return defaultLinks;
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

    /**
     * MCP-specific configuration. {@code application.mcp.audience} is the list of JWT audiences the
     * MCP filter chain will accept — it is deliberately independent of
     * {@code jhipster.security.oauth2.audience} (the session chain's list) so widening one cannot
     * silently widen the other. Only tokens issued to a client that carries one of these audiences
     * are accepted at {@code /mcp} and {@code /mcp/message}.
     */
    public static class Mcp {

        private List<String> audience = new ArrayList<>(List.of("mcp-server"));

        public List<String> getAudience() {
            return audience;
        }

        public void setAudience(List<String> audience) {
            this.audience = audience;
        }
    }

    /**
     * LINK-001: the Atlassian tenant used to build placeholder URLs for the default link slots
     * that the panel offers as add-buttons. Not persisted at node creation; kept in configuration
     * so the hardcoded tenant no longer lives in code (was {@code DefaultNodeLinks.BASE_URL} and
     * the frontend {@code rules.ts}). The current production value is the default.
     */
    public static class DefaultLinks {

        private String baseUrl = "https://ombuto.atlassian.net";

        public String getBaseUrl() {
            return baseUrl;
        }

        public void setBaseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
        }
    }

    // jhipster-needle-application-properties-property-class
}
