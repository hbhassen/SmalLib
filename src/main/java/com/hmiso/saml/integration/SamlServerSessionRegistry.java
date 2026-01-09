package com.hmiso.saml.integration;

import com.hmiso.saml.api.SamlException;
import com.hmiso.saml.api.SamlPrincipal;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Stockage en memoire des sessions serveur SAML.
 */
public class SamlServerSessionRegistry {
    private final Map<String, SamlServerSession> sessions = new ConcurrentHashMap<>();

    public SamlServerSession createSession(SamlPrincipal principal, Instant expiresAt) {
        if (expiresAt == null) {
            throw new SamlException("Expiration de session serveur manquante");
        }
        Instant now = Instant.now();
        if (!expiresAt.isAfter(now)) {
            throw new SamlException("Expiration de session serveur invalide");
        }
        String id = UUID.randomUUID().toString();
        SamlServerSession session = new SamlServerSession(id, principal, now, expiresAt);
        sessions.put(id, session);
        cleanupExpired(now);
        return session;
    }

    public SamlServerSession getSession(String sessionId) {
        if (sessionId == null || sessionId.isBlank()) {
            return null;
        }
        SamlServerSession session = sessions.get(sessionId);
        if (session == null) {
            return null;
        }
        Instant now = Instant.now();
        if (!session.getExpiresAt().isAfter(now)) {
            sessions.remove(sessionId);
            return null;
        }
        cleanupExpired(now);
        return session;
    }

    public void invalidate(String sessionId) {
        if (sessionId == null || sessionId.isBlank()) {
            return;
        }
        sessions.remove(sessionId);
    }

    private void cleanupExpired(Instant now) {
        sessions.entrySet().removeIf(entry -> !entry.getValue().getExpiresAt().isAfter(now));
    }
}
