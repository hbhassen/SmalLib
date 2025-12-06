package com.hmiso.saml.saml;

import com.hmiso.saml.TestConfigurations;
import com.hmiso.saml.config.BindingType;
import com.hmiso.saml.config.SamlConfiguration;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertTrue;

class AuthnRequestBuilderTest {

    @Test
    void shouldBuildRequestWithConfiguredEndpoints_TC_SAML_01() {
        SamlConfiguration configuration = TestConfigurations.minimalConfig(BindingType.HTTP_REDIRECT);
        AuthnRequestBuilder builder = new AuthnRequestBuilder(configuration);

        String xml = builder.build("REQ-123", Instant.parse("2024-01-01T00:00:00Z"));

        assertTrue(xml.contains("REQ-123"));
        assertTrue(xml.contains(configuration.getIdentityProvider().getSingleSignOnServiceUrl().toString()));
        assertTrue(xml.contains(configuration.getServiceProvider().getAssertionConsumerServiceUrl().toString()));
    }
}
