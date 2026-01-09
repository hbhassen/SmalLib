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
        SamlResponseValidationContext context = new SamlResponseValidationContext(
                configuration.getServiceProvider().getEntityId(),
                configuration.getServiceProvider().getAssertionConsumerServiceUrl().toString(),
                "REQ-123",
                "REQ-123",
                now.minusSeconds(5),
                now.plusSeconds(5),
                configuration.getIdentityProvider().getEntityId(),
                configuration.getServiceProvider().getAssertionConsumerServiceUrl().toString(),
                true,
                true
        );
        assertDoesNotThrow(() -> validator.validate(context));
    }

    @Test
    void shouldRejectInvalidAudienceOrWindow_TC_SAML_02() {
        SamlConfiguration configuration = TestConfigurations.minimalConfig(BindingType.HTTP_POST);
        SamlResponseValidator validator = new SamlResponseValidator(configuration);

        Instant now = Instant.parse("2024-01-01T00:00:00Z");

        SamlResponseValidationContext badAudience = new SamlResponseValidationContext(
                "other-entity",
                configuration.getServiceProvider().getAssertionConsumerServiceUrl().toString(),
                "REQ-123",
                "REQ-123",
                now.minusSeconds(30),
                now.plusSeconds(30),
                configuration.getIdentityProvider().getEntityId(),
                configuration.getServiceProvider().getAssertionConsumerServiceUrl().toString(),
                true,
                true
        );
        assertThrows(SamlException.class, () -> validator.validate(badAudience));

        SamlResponseValidationContext expired = new SamlResponseValidationContext(
                configuration.getServiceProvider().getEntityId(),
                configuration.getServiceProvider().getAssertionConsumerServiceUrl().toString(),
                "REQ-123",
                "REQ-123",
                now.minusSeconds(300),
                now.minusSeconds(200),
                configuration.getIdentityProvider().getEntityId(),
                configuration.getServiceProvider().getAssertionConsumerServiceUrl().toString(),
                true,
                true
        );
        assertThrows(SamlException.class, () -> validator.validate(expired));
    }
}
