package com.hmiso.examples.demo2;

import com.hmiso.saml.binding.RelayStateManager;
import com.hmiso.saml.binding.RelayStateStore;
import com.hmiso.saml.integration.DefaultSamlAuditLogger;
import com.hmiso.saml.integration.DefaultSamlErrorHandler;
import com.hmiso.saml.integration.SamlAuthenticationFilterConfig;
import com.hmiso.saml.integration.SamlAuthenticationFilterHelper;
import com.hmiso.saml.integration.SamlSessionHelper;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Clock;
import java.time.Duration;
import java.util.List;

@WebListener
public class SamlBootstrapListener implements ServletContextListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(SamlBootstrapListener.class);

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        LOGGER.info("Initialisation du contexte SAML demo2");
        RelayStateStore relayStateStore = new RelayStateManager(Duration.ofMinutes(5), Clock.systemUTC());

        String contextPath = sce.getServletContext().getContextPath();
        String basePath = (contextPath == null || contextPath.isBlank() || "/".equals(contextPath)) ? "" : contextPath;
        String protectedApiPath = basePath + "/api/*";
        String acsPath = basePath + "/login/saml2/sso/acs";
        String sloPath = basePath + "/logout/saml";

        SamlAuthenticationFilterConfig filterConfig = SamlAuthenticationFilterConfig.builder()
                // Utilise le contextPath du WAR pour matcher /demo2/api/* lors du dploiement
                .protectedPaths(List.of(protectedApiPath))
                .acsPath(acsPath)
                .sloPath(sloPath)
                .sessionAttributeKey(SamlDemo2Configuration.SESSION_ATTRIBUTE_KEY)
                .samlServiceProvider(SamlDemo2Configuration.buildServiceProvider(basePath))
                .relayStateStore(relayStateStore)
                .build();

        SamlAuthenticationFilterHelper helper = new SamlAuthenticationFilterHelper(
                filterConfig,
                new SamlSessionHelper(),
                new DefaultSamlAuditLogger(),
                new DefaultSamlErrorHandler("/saml/error")
        );

        sce.getServletContext().setAttribute(SamlJakartaFilter.CONFIG_KEY, filterConfig);
        sce.getServletContext().setAttribute(SamlJakartaFilter.HELPER_KEY, helper);
    }
}
