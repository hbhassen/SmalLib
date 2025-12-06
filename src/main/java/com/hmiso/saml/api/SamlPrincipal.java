package com.hmiso.saml.api;

import java.io.Serializable;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;

/**
 * Représente l'utilisateur authentifié extrait d'une assertion SAML.
 */
public final class SamlPrincipal implements Serializable {
    private final String nameId;
    private final String email;
    private final Map<String, Object> attributes;
    private final String sessionIndex;

    public SamlPrincipal(String nameId, String email, String sessionIndex, Map<String, Object> attributes) {
        this.nameId = Objects.requireNonNull(nameId, "nameId");
        this.email = email;
        this.sessionIndex = sessionIndex;
        this.attributes = attributes == null ? Collections.emptyMap() : Collections.unmodifiableMap(attributes);
    }

    public String getNameId() {
        return nameId;
    }

    public String getEmail() {
        return email;
    }

    public Map<String, Object> getAttributes() {
        return attributes;
    }

    public String getSessionIndex() {
        return sessionIndex;
    }
}
