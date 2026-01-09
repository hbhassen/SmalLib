package com.hmiso.saml.api;

import com.hmiso.saml.binding.BindingMessage;
import com.hmiso.saml.config.SamlConfiguration;

import java.util.Objects;

/**
 * API publique principale pour gérer AuthnRequest, SAMLResponse et SLO.
 */
public interface SamlServiceProvider {
    SamlConfiguration getConfiguration();

    BindingMessage initiateAuthentication(String relayState);

    SamlPrincipal processSamlResponse(String samlResponse, String relayState);

    BindingMessage initiateLogout(String sessionIndex, String relayState);

    default BindingMessage initiateLogout(SamlPrincipal principal, String relayState) {
        Objects.requireNonNull(principal, "principal");
        return initiateLogout(principal.getSessionIndex(), relayState);
    }

    void processLogoutResponse(String logoutResponse, String inResponseTo);
}
