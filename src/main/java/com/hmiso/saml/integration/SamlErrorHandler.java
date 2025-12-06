package com.hmiso.saml.integration;

/**
 * Point d'extension pour mapper les erreurs SAML vers des codes applicatifs (VII - Error Handling).
 */
public interface SamlErrorHandler {
    String handleValidationError(Throwable error);

    String handleSecurityError(Throwable error);

    String handleBindingError(Throwable error);
}
