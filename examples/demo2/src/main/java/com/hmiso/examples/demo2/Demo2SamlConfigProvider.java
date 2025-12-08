package com.hmiso.examples.demo2;

import com.hmiso.saml.binding.RelayStateManager;
import com.hmiso.saml.binding.RelayStateStore;
import com.hmiso.saml.integration.SamlAuthenticationFilterConfig;
import com.hmiso.saml.integration.jakarta.SamlConfigProvider;
import jakarta.servlet.ServletContext;

import java.time.Clock;
import java.time.Duration;
import java.util.List;

/**
 * Fournit la configuration SAML pour demo2 au listener SmalLib.
 */
public class Demo2SamlConfigProvider implements SamlConfigProvider {
    @Override
    public SamlAuthenticationFilterConfig provide(ServletContext servletContext) {
        RelayStateStore relayStateStore = new RelayStateManager(Duration.ofMinutes(5), Clock.systemUTC());

        String contextPath = servletContext.getContextPath();
        String basePath = (contextPath == null || contextPath.isBlank() || "/".equals(contextPath)) ? "" : contextPath;
        String protectedApiPath = basePath + "/api/*";
        String acsPath = basePath + "/login/saml2/sso/acs";
        String sloPath = basePath + "/logout/saml";

        return SamlAuthenticationFilterConfig.builder()
                .protectedPaths(List.of(protectedApiPath))
                .acsPath(acsPath)
                .sloPath(sloPath)
                .sessionAttributeKey(SamlDemo2Configuration.SESSION_ATTRIBUTE_KEY)
                .samlServiceProvider(SamlDemo2Configuration.buildServiceProvider(basePath))
                .relayStateStore(relayStateStore)
                .build();
    }
}
