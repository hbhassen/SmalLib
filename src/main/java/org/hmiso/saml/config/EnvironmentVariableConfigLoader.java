package org.hmiso.saml.config;

import java.util.Map;
import java.util.Objects;

/**
 * Loader basé sur les variables d'environnement, avec substitution simple.
 */
public class EnvironmentVariableConfigLoader extends AbstractConfigLoader {
    private final SamlConfiguration configuration;
    private final Map<String, String> environment;

    public EnvironmentVariableConfigLoader(SamlConfiguration configuration, Map<String, String> environment) {
        this.configuration = Objects.requireNonNull(configuration, "configuration");
        this.environment = Objects.requireNonNull(environment, "environment");
    }

    @Override
    protected SamlConfiguration doLoad() {
        return configuration;
    }

    public Map<String, String> getEnvironment() {
        return environment;
    }
}
