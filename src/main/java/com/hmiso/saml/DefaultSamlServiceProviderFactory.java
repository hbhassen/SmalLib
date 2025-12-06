package com.hmiso.saml;

import com.hmiso.saml.api.SamlServiceProvider;
import com.hmiso.saml.api.SamlServiceProviderFactory;
import com.hmiso.saml.config.ConfigLoader;
import com.hmiso.saml.config.SamlConfiguration;

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
