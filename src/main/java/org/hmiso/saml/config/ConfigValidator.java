package org.hmiso.saml.config;

import java.net.URI;

/**
 * Validation minimale conforme aux exigences de SPECIFICATION.md.
 */
public final class ConfigValidator {
    private ConfigValidator() {
    }

    public static void requireHttps(URI uri, boolean forceHttpsRedirect) {
        if (forceHttpsRedirect && uri != null && !"https".equalsIgnoreCase(uri.getScheme())) {
            throw new ConfigValidationException("ACS ou endpoint doit être en HTTPS lorsque forceHttpsRedirect=true");
        }
    }

    public static void requireSupportedBinding(BindingType bindingType) {
        if (bindingType == null) {
            throw new ConfigValidationException("Un binding SAML supporté doit être spécifié");
        }
    }
}
