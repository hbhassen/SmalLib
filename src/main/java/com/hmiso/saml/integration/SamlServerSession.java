package com.hmiso.saml.integration;

import com.hmiso.saml.api.SamlPrincipal;

import java.time.Instant;
import java.util.Objects;

/**
 * Session serveur SAML (reference d'autorite cote SP).
 */
public final class SamlServerSession {
    private final String sessionId;
    private final SamlPrincipal principal;
    private final Instant issuedAt;
    private final Instant expiresAt;

    public SamlServerSession(String sessionId, SamlPrincipal principal, Instant issuedAt, Instant expiresAt) {
        this.sessionId = Objects.requireNonNull(sessionId, "sessionId");
        this.principal = Objects.requireNonNull(principal, "principal");
        this.issuedAt = Objects.requireNonNull(issuedAt, "issuedAt");
        this.expiresAt = Objects.requireNonNull(expiresAt, "expiresAt");
    }

    public String getSessionId() {
        return sessionId;
    }

    public SamlPrincipal getPrincipal() {
        return principal;
    }

    public Instant getIssuedAt() {
        return issuedAt;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }
}
