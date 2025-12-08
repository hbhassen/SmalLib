package org.hmiso.saml.integration;

import org.hmiso.saml.api.SamlServiceProvider;
import org.hmiso.saml.binding.RelayStateStore;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Configuration immutable du filtre SAML pour aligner les endpoints protégés, ACS et SLO.
 * Chaque attribut documente l'exigence d'intégration WildFly/Servlet du module VII.
 */
public final class SamlAuthenticationFilterConfig {
    private final List<String> protectedPaths;
    private final String acsPath;
    private final String sloPath;
    private final SamlServiceProvider samlServiceProvider;
    private final String sessionAttributeKey;
    private final RelayStateStore relayStateStore;

    private SamlAuthenticationFilterConfig(Builder builder) {
        this.protectedPaths = Collections.unmodifiableList(new ArrayList<>(builder.protectedPaths));
        this.acsPath = builder.acsPath;
        this.sloPath = builder.sloPath;
        this.samlServiceProvider = builder.samlServiceProvider;
        this.sessionAttributeKey = builder.sessionAttributeKey;
        this.relayStateStore = builder.relayStateStore;
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

    public SamlServiceProvider getSamlServiceProvider() {
        return samlServiceProvider;
    }

    public String getSessionAttributeKey() {
        return sessionAttributeKey;
    }

    public RelayStateStore getRelayStateStore() {
        return relayStateStore;
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder fluide pour aligner facilement la configuration du filtre SAML (VII - Servlet Filter Utilities).
     */
    public static final class Builder {
        private final List<String> protectedPaths = new ArrayList<>();
        private String acsPath;
        private String sloPath;
        private SamlServiceProvider samlServiceProvider;
        private String sessionAttributeKey = "saml.principal";
        private RelayStateStore relayStateStore;

        private Builder() {
        }

        public Builder addProtectedPath(String protectedPath) {
            this.protectedPaths.add(Objects.requireNonNull(protectedPath, "protectedPath"));
            return this;
        }

        public Builder protectedPaths(List<String> protectedPaths) {
            this.protectedPaths.clear();
            if (protectedPaths != null) {
                this.protectedPaths.addAll(protectedPaths);
            }
            return this;
        }

        public Builder acsPath(String acsPath) {
            this.acsPath = Objects.requireNonNull(acsPath, "acsPath");
            return this;
        }

        public Builder sloPath(String sloPath) {
            this.sloPath = Objects.requireNonNull(sloPath, "sloPath");
            return this;
        }

        public Builder samlServiceProvider(SamlServiceProvider provider) {
            this.samlServiceProvider = Objects.requireNonNull(provider, "provider");
            return this;
        }

        public Builder sessionAttributeKey(String key) {
            this.sessionAttributeKey = Objects.requireNonNull(key, "sessionAttributeKey");
            return this;
        }

        public Builder relayStateStore(RelayStateStore relayStateStore) {
            this.relayStateStore = Objects.requireNonNull(relayStateStore, "relayStateStore");
            return this;
        }

        public SamlAuthenticationFilterConfig build() {
            Objects.requireNonNull(acsPath, "acsPath is required");
            Objects.requireNonNull(sloPath, "sloPath is required");
            Objects.requireNonNull(samlServiceProvider, "samlServiceProvider is required");
            Objects.requireNonNull(relayStateStore, "relayStateStore is required");
            return new SamlAuthenticationFilterConfig(this);
        }
    }
}
