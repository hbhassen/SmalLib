package org.hmiso.saml.integration;

import org.hmiso.saml.api.SamlPrincipal;
import org.hmiso.saml.binding.BindingMessage;

/**
 * Interface d'audit pour les evenements SAML majeurs (VII - Logging & Audit).
 */
public interface SamlAuditLogger {
    void logAuthnRequestInitiated(BindingMessage authnRequest);

    void logAuthenticationSuccess(SamlPrincipal principal);

    void logAuthenticationFailure(Exception error);

    void logLogoutInitiated(SamlPrincipal principal);

    void logLogoutSuccess(String sessionIndex);
}
