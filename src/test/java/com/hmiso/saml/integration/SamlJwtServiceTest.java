package com.hmiso.saml.integration;

import com.hmiso.saml.api.SamlPrincipal;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SamlJwtServiceTest {

    @Test
    void issueAndValidateToken() {
        SamlJwtService service = new SamlJwtService("test-secret");
        SamlPrincipal principal = new SamlPrincipal("user", null, "sess", Map.of());
        SamlServerSession session = new SamlServerSession("sid-1", principal, Instant.now(), Instant.now().plusSeconds(60));

        String token = service.issueToken(session, Duration.ofSeconds(10), List.of("role1"));
        assertNotNull(token);

        SamlJwtService.JwtClaims claims = service.validate(token);
        assertEquals("user", claims.getSubject());
        assertEquals("sid-1", claims.getSessionId());
        assertEquals(List.of("role1"), claims.getRoles());
    }

    @Test
    void rejectsInvalidToken() {
        SamlJwtService service = new SamlJwtService("test-secret");
        assertThrows(com.hmiso.saml.api.SamlException.class, () -> service.validate("not-a-jwt"));
    }
}
