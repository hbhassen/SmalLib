package org.wildfly.security.auth.principal;

import java.io.Serial;
import java.io.Serializable;
import java.security.Principal;
import java.util.Objects;

/**
 * Substitut léger de NamePrincipal pour mapper un identifiant SAML vers WildFly (VII - WildFlySecurityMappingHelper).
 */
public class NamePrincipal implements Principal, Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private final String name;

    public NamePrincipal(String name) {
        this.name = Objects.requireNonNull(name, "name");
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof NamePrincipal that)) return false;
        return name.equals(that.name);
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }
}
