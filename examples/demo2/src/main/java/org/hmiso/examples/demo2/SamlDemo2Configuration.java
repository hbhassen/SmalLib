package org.hmiso.examples.demo2;

import org.hmiso.saml.DefaultSamlServiceProviderFactory;
import org.hmiso.saml.api.SamlServiceProvider;
import org.hmiso.saml.api.SamlServiceProviderFactory;
import org.hmiso.saml.config.BindingType;
import org.hmiso.saml.config.IdentityProviderConfig;
import org.hmiso.saml.config.SamlConfiguration;
import org.hmiso.saml.config.SecurityConfig;
import org.hmiso.saml.config.ServiceProviderConfig;

import java.net.URI;
import java.time.Duration;
import java.util.List;

public final class SamlDemo2Configuration {

    public static final String SESSION_ATTRIBUTE_KEY = "saml.principal";

    private SamlDemo2Configuration() {
    }

    public static SamlConfiguration samlConfiguration() {
        return samlConfiguration("/demo2");
    }

    public static SamlConfiguration samlConfiguration(String contextPath) {
        String appContextPath = normalizeContextPath(contextPath);

        ServiceProviderConfig spConfig = new ServiceProviderConfig(
                "saml-sp",
                URI.create("http://localhost:8080" + appContextPath + "/login/saml2/sso/acs"),
                URI.create("http://localhost:8080" + appContextPath + "/logout/saml"),
                "urn:oasis:names:tc:SAML:1.1:nameid-format:emailAddress",
                BindingType.HTTP_POST,
                true,
                List.of("urn:oasis:names:tc:SAML:1.1:nameid-format:emailAddress")
        );

        IdentityProviderConfig idpConfig = new IdentityProviderConfig(
                "saml-realm",
                URI.create("https://localhost:8443/realms/saml-realm/protocol/saml"),
                URI.create("https://localhost:8443/realms/saml-realm/protocol/saml"),
                null,
                null,
                URI.create("https://localhost:8443/realms/saml-realm/protocol/saml/descriptor"),
                true,
                true,
                List.of(BindingType.HTTP_POST, BindingType.HTTP_REDIRECT)
        );

        SecurityConfig securityConfig = new SecurityConfig(
                Duration.ofMinutes(2),
                "rsa-sha256",
                "sha256",
                null,
                null,
                "aes256",
                false,
                true
        );

        return new SamlConfiguration(spConfig, idpConfig, securityConfig);
    }

    public static SamlServiceProvider buildServiceProvider() {
        SamlServiceProviderFactory factory = new DefaultSamlServiceProviderFactory();
        return factory.create(samlConfiguration());
    }

    public static SamlServiceProvider buildServiceProvider(String contextPath) {
        SamlServiceProviderFactory factory = new DefaultSamlServiceProviderFactory();
        return factory.create(samlConfiguration(contextPath));
    }

    private static String normalizeContextPath(String contextPath) {
        if (contextPath == null || contextPath.isBlank() || "/".equals(contextPath)) {
            return "";
        }
        if (!contextPath.startsWith("/")) {
            return "/" + contextPath;
        }
        return contextPath;
    }
}
