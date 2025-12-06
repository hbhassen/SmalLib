package com.hmiso.saml.saml;

import com.hmiso.saml.config.SamlConfiguration;

import java.time.Instant;
import java.util.Objects;

/**
 * Construction minimale d'une AuthnRequest SAML alignée sur la configuration.
 */
public class AuthnRequestBuilder {
    private final SamlConfiguration configuration;

    public AuthnRequestBuilder(SamlConfiguration configuration) {
        this.configuration = Objects.requireNonNull(configuration, "configuration");
    }

    public String build(String requestId, Instant issueInstant) {
        // E7 / TC-SAML-01 : générer une AuthnRequest alignée sur l'ACS et le SSO configurés.
        return "<AuthnRequest ID='" + requestId + "' IssueInstant='" + issueInstant + "' Destination='"
                + configuration.getIdentityProvider().getSingleSignOnServiceUrl() + "' AssertionConsumerServiceURL='"
                + configuration.getServiceProvider().getAssertionConsumerServiceUrl() + "'/>";
    }
}
