package com.hmiso.saml.integration;

import com.hmiso.saml.api.SamlPrincipal;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class SamlServerSessionRegistryTest {

    @Test
    void createGetAndInvalidateSession() {
        SamlServerSessionRegistry registry = new SamlServerSessionRegistry();
        SamlPrincipal principal = new SamlPrincipal("user", null, "sess", Map.of());
        SamlServerSession session = registry.createSession(principal, Instant.now().plusSeconds(30));
        assertNotNull(session);

        SamlServerSession loaded = registry.getSession(session.getSessionId());
        assertNotNull(loaded);

        registry.invalidate(session.getSessionId());
        assertNull(registry.getSession(session.getSessionId()));
    }
}
