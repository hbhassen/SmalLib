package org.hmiso.saml.config;

import java.util.Objects;

/**
 * Configuration complète SmalLib combinant SP, IdP et paramètres sécurité.
 */
public final class SamlConfiguration {
    private final ServiceProviderConfig serviceProvider;
    private final IdentityProviderConfig identityProvider;
    private final SecurityConfig security;

    public SamlConfiguration(ServiceProviderConfig serviceProvider,
                             IdentityProviderConfig identityProvider,
                             SecurityConfig security) {
        this.serviceProvider = Objects.requireNonNull(serviceProvider, "serviceProvider");
        this.identityProvider = Objects.requireNonNull(identityProvider, "identityProvider");
        this.security = Objects.requireNonNull(security, "security");
    }

    public ServiceProviderConfig getServiceProvider() {
        return serviceProvider;
    }

    public IdentityProviderConfig getIdentityProvider() {
        return identityProvider;
    }

    public SecurityConfig getSecurity() {
        return security;
    }
}
