package org.hmiso.saml.api;

/**
 * Exception générique pour les erreurs SAML détectées par SmalLib.
 */
public class SamlException extends RuntimeException {
    public SamlException(String message) {
        super(message);
    }

    public SamlException(String message, Throwable cause) {
        super(message, cause);
    }
}
