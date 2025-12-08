package org.hmiso.saml.config;

import java.util.Objects;

/**
 * Loader YAML (SnakeYAML) — stub pour futures intégrations.
 */
public class YamlConfigLoader extends AbstractConfigLoader {
    private final SamlConfiguration configuration;

    public YamlConfigLoader(SamlConfiguration configuration) {
        this.configuration = Objects.requireNonNull(configuration, "configuration");
    }

    @Override
    protected SamlConfiguration doLoad() {
        return configuration;
    }
}
