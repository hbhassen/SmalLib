package org.hmiso.saml.api;

import org.hmiso.saml.config.ConfigLoader;
import org.hmiso.saml.config.SamlConfiguration;

/**
 * Factory pour produire des instances de {@link SamlServiceProvider} thread-safe.
 */
public interface SamlServiceProviderFactory {
    SamlServiceProvider create(ConfigLoader loader);

    SamlServiceProvider create(SamlConfiguration configuration);
}
