package com.hmiso.saml.integration;

import com.hmiso.saml.api.SamlPrincipal;

/**
 * Interface d'audit pour les événements SAML majeurs.
 */
public interface SamlAuditLogger {
    void onLoginSuccess(SamlPrincipal principal);

    void onLogout(String sessionIndex);

    void onError(String message, Throwable error);
}
