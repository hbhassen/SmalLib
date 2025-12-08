package org.hmiso.saml.metadata;

import org.hmiso.saml.config.BindingType;

import java.net.URI;
import java.util.List;
import java.util.Objects;

/**
 * Analyseur minimal des métadonnées IdP.
 */
public class MetadataParser {
    public MetadataInfo parse(String entityId, URI ssoUrl, URI sloUrl, List<BindingType> bindings) {
        Objects.requireNonNull(entityId, "entityId");
        Objects.requireNonNull(ssoUrl, "ssoUrl");
        Objects.requireNonNull(bindings, "bindings");
        return new MetadataInfo(entityId, ssoUrl, sloUrl, bindings);
    }
}
