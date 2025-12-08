package org.hmiso.saml;

import org.hmiso.saml.config.BindingType;
import org.hmiso.saml.config.IdentityProviderConfig;
import org.hmiso.saml.config.KeystoreConfig;
import org.hmiso.saml.config.SamlConfiguration;
import org.hmiso.saml.config.SecurityConfig;
import org.hmiso.saml.config.ServiceProviderConfig;

import java.net.URI;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.util.List;

/**
 * Génère des configurations minimales de test alignées sur SPECIFICATION.md.
 */
public final class TestConfigurations {
    private TestConfigurations() {
    }

    public static SamlConfiguration minimalConfig(BindingType bindingType) {
        ServiceProviderConfig sp = new ServiceProviderConfig(
                "sp-entity",
                URI.create("https://sp.example.com/acs"),
                URI.create("https://sp.example.com/logout"),
                "urn:oasis:names:tc:SAML:1.1:nameid-format:emailAddress",
                bindingType,
                true,
                List.of("emailAddress")
        );
        IdentityProviderConfig idp = new IdentityProviderConfig(
                "idp-entity",
                URI.create("https://idp.example.com/sso"),
                URI.create("https://idp.example.com/slo"),
                (X509Certificate) null,
                null,
                null,
                true,
                true,
                List.of(bindingType)
        );
        SecurityConfig security = new SecurityConfig(
                Duration.ofMinutes(2),
                "RSA-SHA256",
                "SHA-256",
                (KeystoreConfig) null,
                null,
                null,
                true,
                true
        );
        return new SamlConfiguration(sp, idp, security);
    }
}
