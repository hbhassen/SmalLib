package com.hmiso.saml.integration;

import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletContextEvent;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SamlBootstrapListenerTest {

    private static final String YAML = ""
            + "app:\n"
            + "  protected-paths:\n"
            + "    - \"/api/*\"\n"
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
    void contextInitializedStoresConfigAndHelper() throws Exception {
        Path configPath = Files.createTempFile("saml-config", ".yml");
        Files.writeString(configPath, YAML, StandardCharsets.UTF_8);
        String previous = System.getProperty(SamlAppYamlConfigLoader.CONFIG_PROPERTY);
        System.setProperty(SamlAppYamlConfigLoader.CONFIG_PROPERTY, configPath.toString());

        Map<String, Object> attributes = new HashMap<>();
        ServletContext context = mock(ServletContext.class);
        when(context.getContextPath()).thenReturn("/demo2");
        when(context.getAttribute(anyString())).thenAnswer(invocation -> attributes.get(invocation.getArgument(0)));
        doAnswer(invocation -> {
            attributes.put(invocation.getArgument(0), invocation.getArgument(1));
            return null;
        }).when(context).setAttribute(anyString(), any());

        try {
            new SamlBootstrapListener().contextInitialized(new ServletContextEvent(context));

            assertTrue(attributes.get(SamlAppConfiguration.CONFIG_CONTEXT_KEY) instanceof SamlAppConfiguration);
            assertTrue(attributes.get(SamlAppConfiguration.FILTER_CONFIG_CONTEXT_KEY)
                    instanceof SamlAuthenticationFilterConfig);
            assertTrue(attributes.get(SamlAppConfiguration.HELPER_CONTEXT_KEY)
                    instanceof SamlAuthenticationFilterHelper);
        } finally {
            if (previous == null) {
                System.clearProperty(SamlAppYamlConfigLoader.CONFIG_PROPERTY);
            } else {
                System.setProperty(SamlAppYamlConfigLoader.CONFIG_PROPERTY, previous);
            }
            Files.deleteIfExists(configPath);
        }
    }
}
