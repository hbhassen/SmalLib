package org.hmiso.saml;

import org.hmiso.saml.api.SamlServiceProvider;
import org.hmiso.saml.api.SamlServiceProviderFactory;
import org.hmiso.saml.config.ConfigLoader;
import org.hmiso.saml.config.SamlConfiguration;

import java.util.Objects;

/**
 * Factory simple qui applique les chargeurs de configuration avant d'instancier le provider.
 */
public class DefaultSamlServiceProviderFactory implements SamlServiceProviderFactory {
    @Override
    public SamlServiceProvider create(ConfigLoader loader) {
        Objects.requireNonNull(loader, "loader");
        return create(loader.load());
    }

    @Override
    public SamlServiceProvider create(SamlConfiguration configuration) {
        Objects.requireNonNull(configuration, "configuration");
        return new DefaultSamlServiceProvider(configuration);
    }
}
