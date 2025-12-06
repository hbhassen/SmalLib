package com.hmiso.saml.config;

import java.util.Objects;

/**
 * Loader JSON (Jackson) — ici limité à un stub déterministe.
 */
public class JsonConfigLoader extends AbstractConfigLoader {
    private final SamlConfiguration configuration;

    public JsonConfigLoader(SamlConfiguration configuration) {
        this.configuration = Objects.requireNonNull(configuration, "configuration");
    }

    @Override
    protected SamlConfiguration doLoad() {
        return configuration;
    }
}
