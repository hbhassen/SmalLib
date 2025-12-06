package com.hmiso.saml.metadata;

import com.hmiso.saml.config.BindingType;

import java.net.URI;
import java.util.List;
import java.util.Objects;

/**
 * Résumé des métadonnées IdP utilisées par la configuration.
 */
public final class MetadataInfo {
    private final String entityId;
    private final URI singleSignOnService;
    private final URI singleLogoutService;
    private final List<BindingType> supportedBindings;

    public MetadataInfo(String entityId, URI singleSignOnService, URI singleLogoutService, List<BindingType> supportedBindings) {
        this.entityId = Objects.requireNonNull(entityId, "entityId");
        this.singleSignOnService = Objects.requireNonNull(singleSignOnService, "singleSignOnService");
        this.singleLogoutService = singleLogoutService;
        this.supportedBindings = List.copyOf(Objects.requireNonNull(supportedBindings, "supportedBindings"));
    }

    public String getEntityId() {
        return entityId;
    }

    public URI getSingleSignOnService() {
        return singleSignOnService;
    }

    public URI getSingleLogoutService() {
        return singleLogoutService;
    }

    public List<BindingType> getSupportedBindings() {
        return supportedBindings;
    }
}
