package com.hmiso.saml.integration;

import com.hmiso.saml.config.SamlConfiguration;

import java.time.Duration;
import java.util.List;
import java.util.Objects;

/**
 * Aggregates application-level settings needed to wire the servlet filter.
 */
public final class SamlAppConfiguration {
    public static final String DEFAULT_SESSION_ATTRIBUTE_KEY = "saml.principal";
    public static final String DEFAULT_SERVER_SESSION_ATTRIBUTE_KEY = "saml.server.session";
    public static final String DEFAULT_ERROR_PATH = "/saml/error";
    public static final Duration DEFAULT_RELAY_STATE_TTL = Duration.ofMinutes(5);
    public static final Duration DEFAULT_SESSION_MAX_TTL = Duration.ofMinutes(60);
    public static final Duration DEFAULT_JWT_TTL = Duration.ofSeconds(10);

    public static final String CONFIG_CONTEXT_KEY = "smalib.saml.app.config";
    public static final String FILTER_CONFIG_CONTEXT_KEY = "smalib.saml.filter.config";
    public static final String HELPER_CONTEXT_KEY = "smalib.saml.filter.helper";

    private final SamlConfiguration samlConfiguration;
    private final String sessionAttributeKey;
    private final String serverSessionAttributeKey;
    private final Duration sessionMaxTtl;
    private final List<String> protectedPaths;
    private final String acsPath;
    private final String sloPath;
    private final Duration relayStateTtl;
    private final String errorPath;
    private final Duration jwtTtl;
    private final String jwtSecret;

    public SamlAppConfiguration(SamlConfiguration samlConfiguration,
                                String sessionAttributeKey,
                                String serverSessionAttributeKey,
                                Duration sessionMaxTtl,
                                List<String> protectedPaths,
                                String acsPath,
                                String sloPath,
                                Duration relayStateTtl,
                                String errorPath,
                                Duration jwtTtl,
                                String jwtSecret) {
        this.samlConfiguration = Objects.requireNonNull(samlConfiguration, "samlConfiguration");
        this.sessionAttributeKey = Objects.requireNonNull(sessionAttributeKey, "sessionAttributeKey");
        this.serverSessionAttributeKey = Objects.requireNonNull(serverSessionAttributeKey, "serverSessionAttributeKey");
        this.sessionMaxTtl = Objects.requireNonNull(sessionMaxTtl, "sessionMaxTtl");
        this.protectedPaths = List.copyOf(Objects.requireNonNull(protectedPaths, "protectedPaths"));
        this.acsPath = Objects.requireNonNull(acsPath, "acsPath");
        this.sloPath = Objects.requireNonNull(sloPath, "sloPath");
        this.relayStateTtl = Objects.requireNonNull(relayStateTtl, "relayStateTtl");
        this.errorPath = Objects.requireNonNull(errorPath, "errorPath");
        this.jwtTtl = Objects.requireNonNull(jwtTtl, "jwtTtl");
        this.jwtSecret = jwtSecret;
    }

    public SamlConfiguration getSamlConfiguration() {
        return samlConfiguration;
    }

    public String getSessionAttributeKey() {
        return sessionAttributeKey;
    }

    public String getServerSessionAttributeKey() {
        return serverSessionAttributeKey;
    }

    public Duration getSessionMaxTtl() {
        return sessionMaxTtl;
    }

    public List<String> getProtectedPaths() {
        return protectedPaths;
    }

    public String getAcsPath() {
        return acsPath;
    }

    public String getSloPath() {
        return sloPath;
    }

    public Duration getRelayStateTtl() {
        return relayStateTtl;
    }

    public String getErrorPath() {
        return errorPath;
    }

    public Duration getJwtTtl() {
        return jwtTtl;
    }

    public String getJwtSecret() {
        return jwtSecret;
    }
}
