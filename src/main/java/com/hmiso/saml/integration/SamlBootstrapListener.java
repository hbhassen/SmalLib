package com.hmiso.saml.integration;

import com.hmiso.saml.DefaultSamlServiceProviderFactory;
import com.hmiso.saml.api.SamlServiceProvider;
import com.hmiso.saml.api.SamlServiceProviderFactory;
import com.hmiso.saml.binding.RelayStateManager;
import com.hmiso.saml.binding.RelayStateStore;
import jakarta.security.auth.message.config.AuthConfigFactory;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Clock;
import java.util.UUID;

/**
 * Servlet listener that initializes the SAML filter configuration from YAML.
 */
@WebListener
public class SamlBootstrapListener implements ServletContextListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(SamlBootstrapListener.class);
    private String jaspicRegistrationId;

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        LOGGER.info("Initializing SAML context");

        String contextPath = sce.getServletContext().getContextPath();
        SamlAppYamlConfigLoader loader = new SamlAppYamlConfigLoader();
        SamlAppConfiguration appConfig = loader.load(contextPath);

        RelayStateStore relayStateStore = new RelayStateManager(appConfig.getRelayStateTtl(), Clock.systemUTC());
        SamlServiceProviderFactory factory = new DefaultSamlServiceProviderFactory();
        SamlServiceProvider serviceProvider = factory.create(appConfig.getSamlConfiguration());
        SamlServerSessionRegistry sessionRegistry = new SamlServerSessionRegistry();
        SamlJwtService jwtService = buildJwtService(appConfig.getJwtSecret());

        SamlAuthenticationFilterConfig filterConfig = SamlAuthenticationFilterConfig.builder()
                .protectedPaths(appConfig.getProtectedPaths())
                .acsPath(appConfig.getAcsPath())
                .sloPath(appConfig.getSloPath())
                .sessionAttributeKey(appConfig.getSessionAttributeKey())
                .serverSessionRegistry(sessionRegistry)
                .serverSessionAttributeKey(appConfig.getServerSessionAttributeKey())
                .sessionMaxTtl(appConfig.getSessionMaxTtl())
                .jwtService(jwtService)
                .jwtTtl(appConfig.getJwtTtl())
                .samlServiceProvider(serviceProvider)
                .relayStateStore(relayStateStore)
                .build();

        SamlAuthenticationFilterHelper helper = new SamlAuthenticationFilterHelper(
                filterConfig,
                new SamlSessionHelper(),
                new DefaultSamlAuditLogger(),
                new DefaultSamlErrorHandler(appConfig.getErrorPath())
        );

        sce.getServletContext().setAttribute(SamlAppConfiguration.FILTER_CONFIG_CONTEXT_KEY, filterConfig);
        sce.getServletContext().setAttribute(SamlAppConfiguration.HELPER_CONTEXT_KEY, helper);
        sce.getServletContext().setAttribute(SamlAppConfiguration.CONFIG_CONTEXT_KEY, appConfig);

        if (appConfig.isJaspicEnabled()) {
            registerJaspicProvider(contextPath);
        }
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        if (jaspicRegistrationId == null) {
            return;
        }
        AuthConfigFactory factory = AuthConfigFactory.getFactory();
        if (factory != null) {
            factory.removeRegistration(jaspicRegistrationId);
            LOGGER.info("JASPIC provider removed id={}", jaspicRegistrationId);
        }
    }

    private SamlJwtService buildJwtService(String jwtSecret) {
        String secret = jwtSecret;
        if (secret == null || secret.isBlank()) {
            secret = UUID.randomUUID() + "-" + UUID.randomUUID();
            LOGGER.warn("JWT secret absent, generation d'une valeur ephemere");
        }
        return new SamlJwtService(secret);
    }

    private void registerJaspicProvider(String contextPath) {
        AuthConfigFactory factory = AuthConfigFactory.getFactory();
        if (factory == null) {
            LOGGER.warn("JASPIC AuthConfigFactory not available");
            return;
        }
        String appContext = contextPath == null ? "" : contextPath;
        jaspicRegistrationId = factory.registerConfigProvider(
                new SamlJaspicAuthConfigProvider(),
                "HttpServlet",
                appContext,
                "SmalLib JASPIC");
        LOGGER.info("JASPIC provider registered id={}", jaspicRegistrationId);
    }
}
