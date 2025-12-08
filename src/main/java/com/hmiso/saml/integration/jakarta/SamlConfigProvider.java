package com.hmiso.saml.integration.jakarta;

import com.hmiso.saml.integration.SamlAuthenticationFilterConfig;
import jakarta.servlet.ServletContext;

/**
 * Point d'extension pour fournir la configuration SAML (SP + chemins proteges) au listener SmalLib.
 * Implementations sont decouvertes via ServiceLoader.
 */
public interface SamlConfigProvider {
    SamlAuthenticationFilterConfig provide(ServletContext servletContext);
}
