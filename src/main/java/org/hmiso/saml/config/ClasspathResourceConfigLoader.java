package org.hmiso.saml.config;

import java.util.Objects;

/**
 * Loader de ressources embarquées dans le classpath.
 */
public class ClasspathResourceConfigLoader extends AbstractConfigLoader {
    private final SamlConfiguration configuration;
    private final String resourcePath;

    public ClasspathResourceConfigLoader(SamlConfiguration configuration, String resourcePath) {
        this.configuration = Objects.requireNonNull(configuration, "configuration");
        this.resourcePath = Objects.requireNonNull(resourcePath, "resourcePath");
    }

    @Override
    protected SamlConfiguration doLoad() {
        return configuration;
    }

    public String getResourcePath() {
        return resourcePath;
    }
}
