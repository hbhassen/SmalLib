package org.hmiso.saml.integration;

import org.hmiso.saml.api.SamlPrincipal;
import org.hmiso.saml.binding.BindingMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implémentation SLF4J du journal d'audit (VII - DefaultSamlAuditLogger).
 */
public class DefaultSamlAuditLogger implements SamlAuditLogger {
    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultSamlAuditLogger.class);

    @Override
    public void logAuthnRequestInitiated(BindingMessage authnRequest) {
        LOGGER.info("AuthnRequest initiated toward IdP binding {} with relayState {}", authnRequest.getBindingType(), authnRequest.getRelayState());
    }

    @Override
    public void logAuthenticationSuccess(SamlPrincipal principal) {
        LOGGER.info("Authentication success for subject {}", principal.getNameId());
    }

    @Override
    public void logAuthenticationFailure(Exception error) {
        LOGGER.warn("Authentication failure: {}", error.getMessage(), error);
    }

    @Override
    public void logLogoutInitiated(SamlPrincipal principal) {
        LOGGER.info("Logout initiated for session {}", principal.getSessionIndex());
    }

    @Override
    public void logLogoutSuccess(String sessionIndex) {
        LOGGER.info("Logout success for session {}", sessionIndex);
    }
}
