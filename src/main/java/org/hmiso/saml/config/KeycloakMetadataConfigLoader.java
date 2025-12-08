package org.hmiso.saml.config;

import java.net.URI;
import java.util.Objects;

/**
 * Loader basé sur les métadonnées Keycloak (EntityDescriptor XML).
 */
public class KeycloakMetadataConfigLoader extends AbstractConfigLoader {
    private final SamlConfiguration configuration;
    private final URI metadataUri;

    public KeycloakMetadataConfigLoader(SamlConfiguration configuration, URI metadataUri) {
        this.configuration = Objects.requireNonNull(configuration, "configuration");
        this.metadataUri = Objects.requireNonNull(metadataUri, "metadataUri");
    }

    @Override
    protected SamlConfiguration doLoad() {
        return configuration;
    }

    public URI getMetadataUri() {
        return metadataUri;
    }
}
