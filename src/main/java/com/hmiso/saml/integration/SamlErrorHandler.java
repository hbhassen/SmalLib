package com.hmiso.saml.integration;

/**
 * Point d'extension pour mapper les erreurs SAML vers des codes applicatifs.
 */
public interface SamlErrorHandler {
    void handleError(String message, Throwable error);
}
