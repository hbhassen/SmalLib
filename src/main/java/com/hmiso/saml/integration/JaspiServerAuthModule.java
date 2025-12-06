package com.hmiso.saml.integration;

import com.hmiso.saml.api.SamlPrincipal;
import com.hmiso.saml.api.SamlServiceProvider;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Module d'authentification JASPIC simplifié (sans dépendance au conteneur servlet).
 */
public class JaspiServerAuthModule {
    private final SamlServiceProvider serviceProvider;

    public JaspiServerAuthModule(SamlServiceProvider serviceProvider) {
        this.serviceProvider = Objects.requireNonNull(serviceProvider, "serviceProvider");
    }

    public Set<String> authenticate(String samlResponse, String relayState) {
        SamlPrincipal principal = serviceProvider.processSamlResponse(samlResponse, relayState);
        Set<String> roles = new HashSet<>();
        principal.getAttributes().forEach((key, value) -> roles.add(key + "=" + value));
        roles.add("principal:" + principal.getNameId());
        return roles;
    }
}
