package com.hmiso.saml.config;

/**
 * Exception dédiée à la validation stricte des configurations SAML.
 */
public class ConfigValidationException extends RuntimeException {
    public ConfigValidationException(String message) {
        super(message);
    }
}
