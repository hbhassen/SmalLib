package com.hmiso.saml.integration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Gestionnaire d'erreurs par défaut qui redirige vers /saml/error (VII - DefaultSamlErrorHandler).
 */
public class DefaultSamlErrorHandler implements SamlErrorHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultSamlErrorHandler.class);
    private final String errorPath;

    public DefaultSamlErrorHandler() {
        this("/saml/error");
    }

    public DefaultSamlErrorHandler(String errorPath) {
        this.errorPath = errorPath;
    }

    @Override
    public String handleValidationError(Throwable error) {
        log("validation", error);
        return errorPath;
    }

    @Override
    public String handleSecurityError(Throwable error) {
        log("security", error);
        return errorPath;
    }

    @Override
    public String handleBindingError(Throwable error) {
        log("binding", error);
        return errorPath;
    }

    private void log(String type, Throwable error) {
        LOGGER.warn("SAML {} error routed to {}: {}", type, errorPath, error.getMessage(), error);
    }
}
