package com.hmiso.saml.integration;

import com.hmiso.saml.api.SamlPrincipal;
import com.hmiso.saml.api.SamlServiceProvider;
import com.hmiso.saml.api.SamlServiceProviderFactory;
import com.hmiso.saml.binding.BindingMessage;

import java.util.Objects;

/**
 * Filtre simplifié permettant de déclencher AuthnRequest ou de traiter SAMLResponse.
 */
public class SamlFilter {
    private final SamlServiceProviderFactory factory;
    private final SamlAuditLogger auditLogger;
    private final SamlErrorHandler errorHandler;

    public SamlFilter(SamlServiceProviderFactory factory, SamlAuditLogger auditLogger, SamlErrorHandler errorHandler) {
        this.factory = Objects.requireNonNull(factory, "factory");
        this.auditLogger = auditLogger;
        this.errorHandler = errorHandler;
    }

    public BindingMessage onProtectedRequest(SamlServiceProvider provider, String relayState) {
        return provider.initiateAuthentication(relayState);
    }

    public SamlPrincipal onAcsResponse(SamlServiceProvider provider, String samlResponse, String relayState) {
        try {
            SamlPrincipal principal = provider.processSamlResponse(samlResponse, relayState);
            if (auditLogger != null) {
                auditLogger.onLoginSuccess(principal);
            }
            return principal;
        } catch (Exception ex) {
            if (errorHandler != null) {
                errorHandler.handleError("Erreur ACS", ex);
            }
            throw ex;
        }
    }
}
