package com.hmiso.saml.integration;

import com.hmiso.saml.config.SamlConfiguration;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SamlAppYamlConfigLoaderTest {

    private static final String FULL_YAML = ""
            + "app:\n"
            + "  protected-paths:\n"
            + "    - \"/api/*\"\n"
            + "  session-attribute-key: \"saml.principal\"\n"
            + "  server-session-attribute-key: \"saml.server.session\"\n"
            + "  session-max-minutes: 45\n"
            + "  error-path: \"/saml/error\"\n"
            + "  relay-state-ttl-minutes: 15\n"
            + "\n"
            + "service-provider:\n"
            + "  entity-id: \"saml-sp\"\n"
            + "  base-url: \"http://localhost:8080\"\n"
            + "  acs-path: \"/login/saml2/sso/acs\"\n"
            + "  slo-path: \"/logout/saml\"\n"
            + "  name-id-format: \"urn:oasis:names:tc:SAML:1.1:nameid-format:emailAddress\"\n"
            + "  authn-request-binding: \"HTTP_POST\"\n"
            + "  want-assertions-signed: true\n"
            + "  supported-name-id-formats:\n"
            + "    - \"urn:oasis:names:tc:SAML:1.1:nameid-format:emailAddress\"\n"
            + "\n"
            + "identity-provider:\n"
            + "  entity-id: \"saml-realm\"\n"
            + "  single-sign-on-service-url: \"https://idp.example.com/sso\"\n"
            + "  single-logout-service-url: \"https://idp.example.com/slo\"\n"
            + "  metadata-url: \"https://idp.example.com/metadata\"\n"
            + "  want-assertions-signed: true\n"
            + "  want-messages-signed: true\n"
            + "  supported-bindings:\n"
            + "    - \"HTTP_POST\"\n"
            + "    - \"HTTP_REDIRECT\"\n"
            + "\n"
            + "security:\n"
            + "  clock-skew: \"PT2M\"\n"
            + "  signature-algorithm: \"rsa-sha256\"\n"
            + "  digest-algorithm: \"sha256\"\n"
            + "  encryption-algorithm: \"aes256\"\n"
            + "  force-https-redirect: false\n"
            + "  enable-detailed-logging: true\n"
            + "  jwt-ttl-seconds: 12\n"
            + "  jwt-secret: \"unit-test-secret\"\n";

    private static final String MISSING_PROTECTED_PATHS_YAML = ""
            + "app:\n"
            + "  session-attribute-key: \"saml.principal\"\n"
            + "  error-path: \"/saml/error\"\n"
            + "\n"
            + "service-provider:\n"
            + "  entity-id: \"saml-sp\"\n"
            + "  base-url: \"http://localhost:8080\"\n"
            + "  acs-path: \"/login/saml2/sso/acs\"\n"
            + "  slo-path: \"/logout/saml\"\n"
            + "  name-id-format: \"urn:oasis:names:tc:SAML:1.1:nameid-format:emailAddress\"\n"
            + "  authn-request-binding: \"HTTP_POST\"\n"
            + "  want-assertions-signed: true\n"
            + "\n"
            + "identity-provider:\n"
            + "  entity-id: \"saml-realm\"\n"
            + "  single-sign-on-service-url: \"https://idp.example.com/sso\"\n"
            + "  single-logout-service-url: \"https://idp.example.com/slo\"\n"
            + "  want-assertions-signed: true\n"
            + "  want-messages-signed: true\n"
            + "  supported-bindings:\n"
            + "    - \"HTTP_POST\"\n"
            + "\n"
            + "security:\n"
            + "  clock-skew: \"PT2M\"\n"
            + "  signature-algorithm: \"rsa-sha256\"\n"
            + "  digest-algorithm: \"sha256\"\n";

    @Test
    void loadFromSystemPropertyAndPrefixesContextPath() throws Exception {
        Path configPath = writeTempConfig(FULL_YAML);
        String previous = System.getProperty(SamlAppYamlConfigLoader.CONFIG_PROPERTY);
        System.setProperty(SamlAppYamlConfigLoader.CONFIG_PROPERTY, configPath.toString());
        try {
            SamlAppConfiguration config = new SamlAppYamlConfigLoader().load("/demo2");
            assertEquals("/demo2/login/saml2/sso/acs", config.getAcsPath());
            assertEquals("/demo2/logout/saml", config.getSloPath());
            assertEquals("/demo2/api/*", config.getProtectedPaths().get(0));
            assertEquals("/demo2/saml/error", config.getErrorPath());
            assertEquals(Duration.ofMinutes(15), config.getRelayStateTtl());
            assertEquals("saml.server.session", config.getServerSessionAttributeKey());
            assertEquals(Duration.ofMinutes(45), config.getSessionMaxTtl());
            assertEquals(Duration.ofSeconds(12), config.getJwtTtl());
            assertEquals("unit-test-secret", config.getJwtSecret());

            SamlConfiguration saml = config.getSamlConfiguration();
            assertEquals("http://localhost:8080/demo2/login/saml2/sso/acs",
                    saml.getServiceProvider().getAssertionConsumerServiceUrl().toString());
            assertEquals(URI.create("https://idp.example.com/slo"),
                    saml.getIdentityProvider().getSingleLogoutServiceUrl());
            assertEquals(URI.create("https://idp.example.com/metadata"),
                    saml.getIdentityProvider().getMetadataUrl());
        } finally {
            restoreSystemProperty(previous);
            Files.deleteIfExists(configPath);
        }
    }

    @Test
    void missingProtectedPathsThrows() throws Exception {
        Path configPath = writeTempConfig(MISSING_PROTECTED_PATHS_YAML);
        String previous = System.getProperty(SamlAppYamlConfigLoader.CONFIG_PROPERTY);
        System.setProperty(SamlAppYamlConfigLoader.CONFIG_PROPERTY, configPath.toString());
        try {
            assertThrows(IllegalStateException.class,
                    () -> new SamlAppYamlConfigLoader().load("/demo2"));
        } finally {
            restoreSystemProperty(previous);
            Files.deleteIfExists(configPath);
        }
    }

    private Path writeTempConfig(String yaml) throws Exception {
        Path path = Files.createTempFile("saml-config", ".yml");
        Files.writeString(path, yaml, StandardCharsets.UTF_8);
        return path;
    }

    private void restoreSystemProperty(String previous) {
        if (previous == null) {
            System.clearProperty(SamlAppYamlConfigLoader.CONFIG_PROPERTY);
        } else {
            System.setProperty(SamlAppYamlConfigLoader.CONFIG_PROPERTY, previous);
        }
    }
}
