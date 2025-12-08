package org.hmiso.saml.config;

import org.hmiso.saml.TestConfigurations;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ConfigValidationTest {

    @Test
    void loadShouldValidateBindingAndHttps_TC_CONFIG_01() {
        SamlConfiguration configuration = TestConfigurations.minimalConfig(BindingType.HTTP_REDIRECT);
        JsonConfigLoader loader = new JsonConfigLoader(configuration);

        SamlConfiguration loaded = loader.load();

        assertEquals(configuration.getServiceProvider().getEntityId(), loaded.getServiceProvider().getEntityId());
    }

    @Test
    void loadShouldRejectHttpAcsWhenForceHttps_TC_CONFIG_03() {
        ServiceProviderConfig sp = new ServiceProviderConfig(
                "sp", URI.create("http://sp.example.com/acs"), URI.create("https://sp.example.com/logout"),
                "format", BindingType.HTTP_REDIRECT, true, java.util.List.of("format"));
        IdentityProviderConfig idp = new IdentityProviderConfig(
                "idp", URI.create("https://idp.example.com/sso"), URI.create("https://idp.example.com/slo"),
                null, null, null, true, true, java.util.List.of(BindingType.HTTP_REDIRECT));
        SecurityConfig security = new SecurityConfig(java.time.Duration.ofMinutes(1), "RSA-SHA256", "SHA-256",
                null, null, null, true, true);
        SamlConfiguration configuration = new SamlConfiguration(sp, idp, security);

        PropertiesConfigLoader loader = new PropertiesConfigLoader(configuration);

        assertThrows(ConfigValidationException.class, loader::load);
    }

    @Test
    void envLoaderExposesVariables_TC_CONFIG_02() {
        SamlConfiguration configuration = TestConfigurations.minimalConfig(BindingType.HTTP_POST);
        EnvironmentVariableConfigLoader loader = new EnvironmentVariableConfigLoader(configuration, Map.of("ENV", "value"));

        assertEquals("value", loader.getEnvironment().get("ENV"));
        assertEquals(configuration, loader.load());
    }
}
