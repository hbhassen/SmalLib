package com.hmiso.saml.integration;

import com.hmiso.saml.api.SamlPrincipal;
import org.wildfly.security.auth.principal.NamePrincipal;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Adaptateur pour mapper les rôles SAML vers les rôles WildFly (VII - WildFlySecurityMappingHelper).
 */
public class WildFlySecurityMappingHelper {
    private final Map<String, String> roleMappings;

    public WildFlySecurityMappingHelper() {
        this(Collections.emptyMap());
    }

    public WildFlySecurityMappingHelper(Map<String, String> roleMappings) {
        this.roleMappings = roleMappings == null ? Collections.emptyMap() : roleMappings;
    }

    public Set<String> mapRolesToWildFlyRoles(Set<String> samlRoles) {
        if (samlRoles == null || samlRoles.isEmpty()) {
            return Collections.emptySet();
        }
        Set<String> mapped = new HashSet<>();
        for (String role : samlRoles) {
            mapped.add(roleMappings.getOrDefault(role, role));
        }
        return mapped;
    }

    public NamePrincipal getWildFlyPrincipal(SamlPrincipal principal) {
        Objects.requireNonNull(principal, "principal");
        return new NamePrincipal(principal.getNameId());
    }
}
