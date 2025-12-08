package org.hmiso.saml.integration;

import org.hmiso.saml.api.SamlPrincipal;
import org.hmiso.saml.api.SamlServiceProvider;
import org.hmiso.saml.api.SamlServiceProviderFactory;
import org.hmiso.saml.binding.BindingMessage;

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
        BindingMessage message = provider.initiateAuthentication(relayState);
        if (auditLogger != null) {
            auditLogger.logAuthnRequestInitiated(message);
        }
        return message;
    }

    public SamlPrincipal onAcsResponse(SamlServiceProvider provider, String samlResponse, String relayState) {
        try {
            SamlPrincipal principal = provider.processSamlResponse(samlResponse, relayState);
            if (auditLogger != null) {
                auditLogger.logAuthenticationSuccess(principal);
            }
            return principal;
        } catch (Exception ex) {
            if (auditLogger != null) {
                auditLogger.logAuthenticationFailure(ex);
            }
            if (errorHandler != null) {
                errorHandler.handleValidationError(ex);
            }
            throw ex;
        }
    }
}
