package org.hmiso.saml.config;

/**
 * Gabarit pour les chargeurs : applique validation commune avant usage.
 */
public abstract class AbstractConfigLoader implements ConfigLoader {
    @Override
    public final SamlConfiguration load() {
        SamlConfiguration configuration = doLoad();
        validate(configuration);
        return configuration;
    }

    @Override
    public void validate(SamlConfiguration configuration) {
        if (configuration == null) {
            throw new ConfigValidationException("La configuration SAML ne peut pas être nulle");
        }
        ConfigValidator.requireSupportedBinding(configuration.getServiceProvider().getAuthnRequestBinding());
        ConfigValidator.requireHttps(configuration.getServiceProvider().getAssertionConsumerServiceUrl(),
                configuration.getSecurity().isForceHttpsRedirect());
    }

    protected abstract SamlConfiguration doLoad();
}
