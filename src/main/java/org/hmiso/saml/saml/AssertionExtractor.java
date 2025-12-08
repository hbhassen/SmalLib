package org.hmiso.saml.saml;

import org.hmiso.saml.api.SamlPrincipal;

import java.util.Map;
import java.util.Objects;

/**
 * Conversion d'une assertion validée vers un {@link SamlPrincipal}.
 */
public class AssertionExtractor {
    public SamlPrincipal toPrincipal(String nameId, String email, String sessionIndex, Map<String, Object> attributes) {
        Objects.requireNonNull(nameId, "nameId");
        return new SamlPrincipal(nameId, email, sessionIndex, attributes);
    }
}
