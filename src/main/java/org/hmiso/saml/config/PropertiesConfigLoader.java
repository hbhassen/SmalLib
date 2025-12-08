package org.hmiso.saml.config;

import java.util.Objects;

/**
 * Loader Properties — stub en attente de mappage complet.
 */
public class PropertiesConfigLoader extends AbstractConfigLoader {
    private final SamlConfiguration configuration;

    public PropertiesConfigLoader(SamlConfiguration configuration) {
        this.configuration = Objects.requireNonNull(configuration, "configuration");
    }

    @Override
    protected SamlConfiguration doLoad() {
        return configuration;
    }
}
