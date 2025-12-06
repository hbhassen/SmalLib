package com.hmiso.saml.saml;

import com.hmiso.saml.TestConfigurations;
import com.hmiso.saml.api.SamlException;
import com.hmiso.saml.config.BindingType;
import com.hmiso.saml.config.SamlConfiguration;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SamlResponseValidatorTest {

    @Test
    void shouldValidateTimestampsAndAudience_TC_SAML_02() {
        SamlConfiguration configuration = TestConfigurations.minimalConfig(BindingType.HTTP_REDIRECT);
        SamlResponseValidator validator = new SamlResponseValidator(configuration);

        Instant now = Instant.now();
        assertDoesNotThrow(() -> validator.validate(
                configuration.getServiceProvider().getEntityId(),
                configuration.getServiceProvider().getAssertionConsumerServiceUrl().toString(),
                "REQ-123",
                now.minusSeconds(5),
                now.plusSeconds(5))
        );
    }

    @Test
    void shouldRejectInvalidAudienceOrWindow_TC_SAML_02() {
        SamlConfiguration configuration = TestConfigurations.minimalConfig(BindingType.HTTP_POST);
        SamlResponseValidator validator = new SamlResponseValidator(configuration);

        Instant now = Instant.parse("2024-01-01T00:00:00Z");

        assertThrows(SamlException.class, () -> validator.validate(
                "other-entity",
                configuration.getServiceProvider().getAssertionConsumerServiceUrl().toString(),
                "REQ-123",
                now.minusSeconds(30),
                now.plusSeconds(30))
        );

        assertThrows(SamlException.class, () -> validator.validate(
                configuration.getServiceProvider().getEntityId(),
                configuration.getServiceProvider().getAssertionConsumerServiceUrl().toString(),
                "REQ-123",
                now.minusSeconds(300),
                now.minusSeconds(200))
        );
    }
}
