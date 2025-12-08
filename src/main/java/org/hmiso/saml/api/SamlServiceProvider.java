package org.hmiso.saml.api;

import org.hmiso.saml.binding.BindingMessage;
import org.hmiso.saml.config.SamlConfiguration;

/**
 * API publique principale pour gérer AuthnRequest, SAMLResponse et SLO.
 */
public interface SamlServiceProvider {
    SamlConfiguration getConfiguration();

    BindingMessage initiateAuthentication(String relayState);

    SamlPrincipal processSamlResponse(String samlResponse, String relayState);

    BindingMessage initiateLogout(String sessionIndex, String relayState);

    void processLogoutResponse(String logoutResponse, String inResponseTo);
}
