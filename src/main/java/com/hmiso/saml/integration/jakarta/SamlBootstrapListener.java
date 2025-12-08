package com.hmiso.saml.integration.jakarta;

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

import java.util.ServiceLoader;

/**
 * Listener generique SmalLib qui bootstrap le helper et le filtre Jakarta.
 * Il s'appuie sur un {@link SamlConfigProvider} charge via ServiceLoader.
 */
@WebListener
public class SamlBootstrapListener implements ServletContextListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(SamlBootstrapListener.class);

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        SamlConfigProvider provider = ServiceLoader.load(SamlConfigProvider.class)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Aucun SamlConfigProvider trouvé via ServiceLoader"));

        SamlAuthenticationFilterConfig filterConfig = provider.provide(sce.getServletContext());

        SamlAuthenticationFilterHelper helper = new SamlAuthenticationFilterHelper(
                filterConfig,
                new SamlSessionHelper(),
                new DefaultSamlAuditLogger(),
                new DefaultSamlErrorHandler("/saml/error")
        );

        sce.getServletContext().setAttribute(SamlJakartaFilter.CONFIG_KEY, filterConfig);
        sce.getServletContext().setAttribute(SamlJakartaFilter.HELPER_KEY, helper);
        LOGGER.info("SmalLib SAML bootstrap initialisé avec provider {}", provider.getClass().getName());
    }
}
