package com.hmiso.saml.integration;

import com.hmiso.saml.api.SamlPrincipal;
import com.hmiso.saml.binding.BindingMessage;

/**
 * Interface d'audit pour les événements SAML majeurs (VII - Logging & Audit).
 */
public interface SamlAuditLogger {
    void logAuthnRequestInitiated(BindingMessage authnRequest);

    void logAuthenticationSuccess(SamlPrincipal principal);

    void logAuthenticationFailure(Exception error);

    void logLogoutInitiated(SamlPrincipal principal);

    void logLogoutSuccess(String sessionIndex);
}
