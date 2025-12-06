package com.hmiso.saml.integration;

import com.hmiso.saml.api.SamlPrincipal;

import jakarta.servlet.http.HttpSession;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

/**
 * Gestion centralisée de la session HTTP pour stocker et récupérer un {@link SamlPrincipal} (VII - Session Management).
 */
public class SamlSessionHelper {

    public void storePrincipalInSession(HttpSession session, SamlPrincipal principal, String sessionAttributeKey) {
        Objects.requireNonNull(session, "session");
        Objects.requireNonNull(principal, "principal");
        Objects.requireNonNull(sessionAttributeKey, "sessionAttributeKey");
        session.setAttribute(sessionAttributeKey, principal);
    }

    public Optional<SamlPrincipal> retrievePrincipalFromSession(HttpSession session, String sessionAttributeKey) {
        if (session == null) {
            return Optional.empty();
        }
        Object value = session.getAttribute(sessionAttributeKey);
        if (value instanceof SamlPrincipal principal) {
            return Optional.of(principal);
        }
        return Optional.empty();
    }

    public void invalidateSession(HttpSession session) {
        if (session != null) {
            session.invalidate();
        }
    }

    public Duration getSessionRemainingTtl(HttpSession session, SamlPrincipal principal) {
        Objects.requireNonNull(session, "session");
        Objects.requireNonNull(principal, "principal");
        int timeoutSeconds = session.getMaxInactiveInterval();
        if (timeoutSeconds <= 0) {
            return Duration.ZERO;
        }
        long elapsedSeconds = Math.max(0, (Instant.now().toEpochMilli() - session.getLastAccessedTime()) / 1000);
        long remaining = Math.max(0, timeoutSeconds - elapsedSeconds);
        return Duration.ofSeconds(remaining);
    }
}
