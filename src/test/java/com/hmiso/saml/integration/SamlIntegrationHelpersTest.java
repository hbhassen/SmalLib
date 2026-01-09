package com.hmiso.saml.integration;

import com.hmiso.saml.DefaultSamlServiceProvider;
import com.hmiso.saml.TestConfigurations;
import com.hmiso.saml.api.SamlPrincipal;
import com.hmiso.saml.binding.RelayStateManager;
import com.hmiso.saml.config.BindingType;
import com.hmiso.saml.config.SamlConfiguration;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SamlIntegrationHelpersTest {

    @Test
    void shouldTriggerRedirectWhenProtectedAndNoSessionPrincipal() {
        SamlConfiguration configuration = TestConfigurations.minimalConfig(BindingType.HTTP_REDIRECT);
        SamlAuthenticationFilterConfig filterConfig = SamlAuthenticationFilterConfig.builder()
                .protectedPaths(java.util.List.of("/secure/*"))
                .acsPath("/saml/acs")
                .sloPath("/saml/slo")
                .relayStateStore(new RelayStateManager(Duration.ofMinutes(5), Clock.systemUTC()))
                .samlServiceProvider(new DefaultSamlServiceProvider(configuration))
                .build();
        SamlAuthenticationFilterHelper helper = new SamlAuthenticationFilterHelper(filterConfig, new SamlSessionHelper(), null, null);

        var request = mock(jakarta.servlet.http.HttpServletRequest.class);
        when(request.getRequestURI()).thenReturn("/secure/page");
        when(request.getSession(false)).thenReturn(null);

        Optional<com.hmiso.saml.binding.BindingMessage> redirect = helper.shouldRedirectToIdP(request, mock(jakarta.servlet.http.HttpServletResponse.class));

        assertTrue(redirect.isPresent());
        assertEquals(configuration.getIdentityProvider().getSingleSignOnServiceUrl(), redirect.get().getDestination());
    }

    @Test
    void shouldReuseSessionPrincipalWhenPresent() {
        SamlConfiguration configuration = TestConfigurations.minimalConfig(BindingType.HTTP_REDIRECT);
        SamlAuthenticationFilterConfig filterConfig = SamlAuthenticationFilterConfig.builder()
                .protectedPaths(java.util.List.of("/secure/*"))
                .acsPath("/saml/acs")
                .sloPath("/saml/slo")
                .relayStateStore(new RelayStateManager(Duration.ofMinutes(5), Clock.systemUTC()))
                .samlServiceProvider(new DefaultSamlServiceProvider(configuration))
                .build();
        SamlSessionHelper sessionHelper = new SamlSessionHelper();
        SamlAuthenticationFilterHelper helper = new SamlAuthenticationFilterHelper(filterConfig, sessionHelper, null, null);
        var session = mock(jakarta.servlet.http.HttpSession.class);
        Map<String, Object> attributes = new HashMap<>();
        when(session.getAttribute(anyString())).thenAnswer(invocation -> attributes.get(invocation.getArgument(0)));
        doAnswer(invocation -> {
            attributes.put(invocation.getArgument(0), invocation.getArgument(1));
            return null;
        }).when(session).setAttribute(anyString(), any());
        var request = mock(jakarta.servlet.http.HttpServletRequest.class);
        when(request.getRequestURI()).thenReturn("/secure/page");
        when(request.getSession(false)).thenReturn(session);
        sessionHelper.storePrincipalInSession(session, new SamlPrincipal("user", null, "s", Map.of()), filterConfig.getSessionAttributeKey());

        Optional<com.hmiso.saml.binding.BindingMessage> redirect = helper.shouldRedirectToIdP(request, mock(jakarta.servlet.http.HttpServletResponse.class));

        assertTrue(redirect.isEmpty());
        assertTrue(helper.extractPrincipalFromSession(session).isPresent());
    }

    @Test
    void shouldHandleAcsAndRedirectOriginalRelayState() throws Exception {
        SamlConfiguration configuration = TestConfigurations.minimalConfig(BindingType.HTTP_REDIRECT);
        RelayStateManager relayStateStore = new RelayStateManager(Duration.ofMinutes(5), Clock.systemUTC());
        String relayState = "relay";
        relayStateStore.save(relayState, "https://app/secure");
        AtomicBoolean audited = new AtomicBoolean();
        SamlAuditLogger logger = new DefaultSamlAuditLogger() {
            @Override
            public void logAuthenticationSuccess(SamlPrincipal principal) {
                audited.set(true);
                super.logAuthenticationSuccess(principal);
            }
        };
        SamlAuthenticationFilterConfig filterConfig = SamlAuthenticationFilterConfig.builder()
                .protectedPaths(java.util.List.of("/secure/*"))
                .acsPath("/saml/acs")
                .sloPath("/saml/slo")
                .relayStateStore(relayStateStore)
                .samlServiceProvider(new DefaultSamlServiceProvider(configuration))
                .build();
        SamlSessionHelper sessionHelper = new SamlSessionHelper();
        SamlAuthenticationFilterHelper helper = new SamlAuthenticationFilterHelper(filterConfig, sessionHelper, logger, new DefaultSamlErrorHandler());
        var session = mock(jakarta.servlet.http.HttpSession.class);
        Map<String, Object> attributes = new HashMap<>();
        when(session.getAttribute(anyString())).thenAnswer(invocation -> attributes.get(invocation.getArgument(0)));
        doAnswer(invocation -> {
            attributes.put(invocation.getArgument(0), invocation.getArgument(1));
            return null;
        }).when(session).setAttribute(anyString(), any());
        var request = mock(jakarta.servlet.http.HttpServletRequest.class);
        when(request.getRequestURI()).thenReturn("/saml/acs");
        when(request.getParameter("SAMLResponse")).thenReturn("user@example.com");
        when(request.getParameter("RelayState")).thenReturn(relayState);
        when(request.getSession(true)).thenReturn(session);
        var response = mock(jakarta.servlet.http.HttpServletResponse.class);
        AtomicBoolean redirected = new AtomicBoolean();
        doAnswer(invocation -> {
            redirected.set(true);
            return null;
        }).when(response).sendRedirect("https://app/secure");

        SamlPrincipal principal = helper.handleAcsRequest(request, response);

        assertEquals("user@example.com", principal.getNameId());
        assertTrue(redirected.get());
        assertTrue(audited.get());
    }

    @Test
    void shouldHandleSloAndInvalidateSession() throws Exception {
        SamlConfiguration configuration = TestConfigurations.minimalConfig(BindingType.HTTP_REDIRECT);
        SamlAuthenticationFilterConfig filterConfig = SamlAuthenticationFilterConfig.builder()
                .protectedPaths(java.util.List.of("/secure/*"))
                .acsPath("/saml/acs")
                .sloPath("/saml/slo")
                .relayStateStore(new RelayStateManager(Duration.ofMinutes(5), Clock.systemUTC()))
                .samlServiceProvider(new DefaultSamlServiceProvider(configuration))
                .build();
        SamlSessionHelper sessionHelper = new SamlSessionHelper();
        AtomicBoolean logoutAudited = new AtomicBoolean();
        SamlAuditLogger logger = new DefaultSamlAuditLogger() {
            @Override
            public void logLogoutSuccess(String sessionIndex) {
                logoutAudited.set(true);
            }
        };
        SamlAuthenticationFilterHelper helper = new SamlAuthenticationFilterHelper(filterConfig, sessionHelper, logger, new DefaultSamlErrorHandler());
        var session = mock(jakarta.servlet.http.HttpSession.class);
        Map<String, Object> attributes = new HashMap<>();
        AtomicBoolean invalidated = new AtomicBoolean(false);
        when(session.getAttribute(anyString())).thenAnswer(invocation -> attributes.get(invocation.getArgument(0)));
        doAnswer(invocation -> {
            attributes.put(invocation.getArgument(0), invocation.getArgument(1));
            return null;
        }).when(session).setAttribute(anyString(), any());
        doAnswer(invocation -> {
            invalidated.set(true);
            return null;
        }).when(session).invalidate();
        sessionHelper.storePrincipalInSession(session, new SamlPrincipal("user", null, "sess-1", Map.of()), filterConfig.getSessionAttributeKey());
        var request = mock(jakarta.servlet.http.HttpServletRequest.class);
        when(request.getRequestURI()).thenReturn("/saml/slo");
        when(request.getSession(false)).thenReturn(session);
        var response = mock(jakarta.servlet.http.HttpServletResponse.class);
        AtomicBoolean redirected = new AtomicBoolean();
        java.util.concurrent.atomic.AtomicReference<String> redirectTarget = new java.util.concurrent.atomic.AtomicReference<>();
        doAnswer(invocation -> {
            redirectTarget.set(invocation.getArgument(0));
            redirected.set(true);
            return null;
        }).when(response).sendRedirect(anyString());

        helper.handleSloRequest(request, response);

        assertTrue(invalidated.get());
        assertTrue(logoutAudited.get());
        assertTrue(redirected.get());
        assertTrue(redirectTarget.get().startsWith(configuration.getIdentityProvider().getSingleLogoutServiceUrl()
                + "?SAMLRequest="));
    }

    @Test
    void shouldComputeRemainingTtl() {
        var session = mock(jakarta.servlet.http.HttpSession.class);
        when(session.getMaxInactiveInterval()).thenReturn(10);
        when(session.getLastAccessedTime()).thenReturn(System.currentTimeMillis());
        SamlSessionHelper helper = new SamlSessionHelper();

        Duration remaining = helper.getSessionRemainingTtl(session, new SamlPrincipal("user", null, "s", Map.of()));

        assertFalse(remaining.isNegative());
        assertTrue(remaining.toSeconds() <= 10);
    }

    @Test
    void shouldMapRolesForWildFly() {
        WildFlySecurityMappingHelper helper = new WildFlySecurityMappingHelper(Map.of("app.admin", "Admin"));
        Set<String> mapped = helper.mapRolesToWildFlyRoles(Set.of("app.admin", "user"));
        assertTrue(mapped.contains("Admin"));
        assertTrue(mapped.contains("user"));

        SamlPrincipal principal = new SamlPrincipal("bob", null, "idx", Map.of());
        assertEquals("bob", helper.getWildFlyPrincipal(principal).getName());
    }

    @Test
    void shouldPopulateSubjectAndGroups() {
        SamlPrincipal principal = new SamlPrincipal("alice", null, "idx", Map.of());
        SamlServerAuthModuleHelper helper = new SamlServerAuthModuleHelper();

        javax.security.auth.Subject subject = helper.createSubjectFromPrincipal(principal);
        assertFalse(subject.getPrincipals().isEmpty());
        assertTrue(subject.getPublicCredentials().contains(principal));

        assertEquals(2, helper.createGroupsFromRoles(Set.of("r1", "r2")).size());
    }

    @Test
    void shouldCreateJaasLoginContextWithFallback() throws Exception {
        SamlPrincipal principal = new SamlPrincipal("alice", null, "idx", Map.of());
        JaasHelper helper = new JaasHelper(new SamlServerAuthModuleHelper(), () -> null);
        assertDoesNotThrow(() -> helper.createLoginContext(principal, "test"));
    }
}
