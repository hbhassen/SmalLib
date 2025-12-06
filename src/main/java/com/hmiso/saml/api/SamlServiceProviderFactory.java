package com.hmiso.saml.api;

import com.hmiso.saml.config.ConfigLoader;
import com.hmiso.saml.config.SamlConfiguration;

/**
 * Factory pour produire des instances de {@link SamlServiceProvider} thread-safe.
 */
public interface SamlServiceProviderFactory {
    SamlServiceProvider create(ConfigLoader loader);

    SamlServiceProvider create(SamlConfiguration configuration);
}
