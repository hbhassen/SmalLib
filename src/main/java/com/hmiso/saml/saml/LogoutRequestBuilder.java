package com.hmiso.saml.saml;

import com.hmiso.saml.config.SamlConfiguration;

import java.time.Instant;
import java.util.Objects;

/**
 * Construit un LogoutRequest basique pour initier le SLO.
 */
public class LogoutRequestBuilder {
    private final SamlConfiguration configuration;

    public LogoutRequestBuilder(SamlConfiguration configuration) {
        this.configuration = Objects.requireNonNull(configuration, "configuration");
    }

    public String build(String requestId, String nameId, String sessionIndex, Instant issueInstant) {
        return "<LogoutRequest ID='" + requestId + "' NameID='" + nameId + "' SessionIndex='" + sessionIndex
                + "' IssueInstant='" + issueInstant + "' Destination='"
                + configuration.getIdentityProvider().getSingleLogoutServiceUrl() + "'/>";
    }
}
