package org.hmiso.saml.saml;

import org.hmiso.saml.TestConfigurations;
import org.hmiso.saml.api.SamlPrincipal;
import org.hmiso.saml.config.BindingType;
import org.hmiso.saml.config.SamlConfiguration;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class AssertionAndLogoutTest {

    @Test
    void extractorBuildsPrincipal_TC_SAML_03() {
        AssertionExtractor extractor = new AssertionExtractor();
        SamlPrincipal principal = extractor.toPrincipal("user@example.com", "user@example.com", "session", java.util.Map.of("role", "admin"));

        assertEquals("user@example.com", principal.getNameId());
        assertEquals("admin", principal.getAttributes().get("role"));
    }

    @Test
    void logoutResponseValidatorChecksTimestamps_TC_SAML_04() {
        SamlConfiguration configuration = TestConfigurations.minimalConfig(BindingType.HTTP_POST);
        LogoutResponseValidator validator = new LogoutResponseValidator(configuration);

        assertThrows(Exception.class, () -> validator.validate("", Instant.now(), Instant.now()));
        validator.validate("LR-1", Instant.now().minus(Duration.ofSeconds(10)), Instant.now().plus(Duration.ofSeconds(10)));
    }
}
