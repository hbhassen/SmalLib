package com.hmiso.saml.integration;

import com.hmiso.saml.api.SamlAttributeKeys;
import com.hmiso.saml.api.SamlException;
import com.hmiso.saml.api.SamlPrincipal;
import com.hmiso.saml.binding.BindingMessage;
import com.hmiso.saml.config.BindingType;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * Logique centrale du filtre Servlet decrite dans le module VII.
 */
public class SamlAuthenticationFilterHelper {
    private final SamlAuthenticationFilterConfig config;
    private final SamlSessionHelper sessionHelper;
    private final SamlAuditLogger auditLogger;
    private final SamlErrorHandler errorHandler;
    private final Predicate<HttpServletRequest> protectedRequestPredicate;
    private final SamlServerSessionRegistry sessionRegistry;
    private final String serverSessionAttributeKey;
    private final Duration sessionMaxTtl;
    private final SamlJwtService jwtService;
    private final Duration jwtTtl;

    public SamlAuthenticationFilterHelper(SamlAuthenticationFilterConfig config,
                                          SamlSessionHelper sessionHelper,
                                          SamlAuditLogger auditLogger,
                                          SamlErrorHandler errorHandler) {
        this.config = config;
        this.sessionHelper = sessionHelper;
        this.auditLogger = auditLogger;
        this.errorHandler = errorHandler;
        this.protectedRequestPredicate = req -> isProtected(req.getRequestURI(), config.getProtectedPaths());
        this.sessionRegistry = config.getServerSessionRegistry();
        this.serverSessionAttributeKey = config.getServerSessionAttributeKey();
        this.sessionMaxTtl = config.getSessionMaxTtl();
        this.jwtService = config.getJwtService();
        this.jwtTtl = config.getJwtTtl();
    }

    public Optional<BindingMessage> shouldRedirectToIdP(HttpServletRequest request, HttpServletResponse response) {
        if (!protectedRequestPredicate.test(request)) {
            return Optional.empty();
        }
        HttpSession session = request.getSession(false);
        Optional<SamlPrincipal> existing = sessionHelper.retrievePrincipalFromSession(session,
                config.getSessionAttributeKey());
        if (existing.isPresent()) {
            if (isServerSessionValid(session, existing.get())) {
                return Optional.empty();
            }
            sessionHelper.invalidateSession(session);
        }
        String originalUri = request.getRequestURI();
        // Enregistre un relay state serveur (si disponible) pour restaurer l'URL initiale apres ACS.
        if (config.getRelayStateStore() != null) {
            config.getRelayStateStore().save(originalUri, originalUri);
        }
        BindingMessage message = config.getSamlServiceProvider().initiateAuthentication(originalUri);
        if (auditLogger != null) {
            auditLogger.logAuthnRequestInitiated(message);
        }
        return Optional.of(message);
    }

    public SamlPrincipal handleAcsRequest(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String samlResponse = request.getParameter("SAMLResponse");
        String relayState = request.getParameter("RelayState");
        try {
            SamlPrincipal principal = config.getSamlServiceProvider().processSamlResponse(samlResponse, relayState);
            HttpSession session = request.getSession(true);
            sessionHelper.storePrincipalInSession(session, principal, config.getSessionAttributeKey());
            createServerSession(principal, session);
            String target = relayState;
            if (relayState != null && config.getRelayStateStore() != null) {
                String stored = config.getRelayStateStore().get(relayState);
                if (stored != null) {
                    target = stored;
                    config.getRelayStateStore().invalidate(relayState);
                }
            }
            if (auditLogger != null) {
                auditLogger.logAuthenticationSuccess(principal);
            }
            if (target != null && response != null) {
                response.sendRedirect(target);
            }
            return principal;
        } catch (Exception ex) {
            if (auditLogger != null) {
                auditLogger.logAuthenticationFailure(ex);
            }
            if (errorHandler != null) {
                String target = errorHandler.handleValidationError(ex);
                if (response != null) {
                    response.sendRedirect(target);
                }
            }
            throw ex;
        }
    }

    public void handleSloRequest(HttpServletRequest request, HttpServletResponse response) throws IOException {
        HttpSession session = request.getSession(false);
        Optional<SamlPrincipal> principal = sessionHelper.retrievePrincipalFromSession(session, config.getSessionAttributeKey());
        principal.ifPresent(p -> {
            if (auditLogger != null) {
                auditLogger.logLogoutInitiated(p);
            }
            BindingMessage logout = config.getSamlServiceProvider().initiateLogout(p, null);
            if (auditLogger != null) {
                auditLogger.logLogoutSuccess(p.getSessionIndex());
            }
            if (response != null) {
                try {
                    if (logout.getBindingType() == BindingType.HTTP_REDIRECT) {
                        String target = logout.getDestination() + "?SAMLRequest=" + urlEncode(logout.getPayload());
                        if (logout.getRelayState() != null) {
                            target += "&RelayState=" + urlEncode(logout.getRelayState());
                        }
                        response.sendRedirect(target);
                    } else {
                        renderPost(response, logout);
                    }
                } catch (IOException ignored) {
                    // nothing to do
                }
            }
            invalidateServerSession(session);
        });
        sessionHelper.invalidateSession(session);
    }

    public Optional<SamlPrincipal> extractPrincipalFromSession(HttpSession session) {
        return sessionHelper.retrievePrincipalFromSession(session, config.getSessionAttributeKey());
    }

    public boolean validateJwtFromRequest(HttpServletRequest request, HttpServletResponse response) throws IOException {
        if (request == null || response == null) {
            return true;
        }
        if (!protectedRequestPredicate.test(request)) {
            return true;
        }
        String token = request.getHeader(SamlJwtService.DEFAULT_HEADER_NAME);
        if (token == null || token.isBlank()) {
            return true;
        }
        if (jwtService == null || sessionRegistry == null) {
            rejectRequest(response, "JWT non supporte");
            return false;
        }
        try {
            SamlJwtService.JwtClaims claims = jwtService.validate(token);
            SamlServerSession serverSession = sessionRegistry.getSession(claims.getSessionId());
            if (serverSession == null) {
                rejectRequest(response, "Session serveur expiree");
                return false;
            }
            String subject = claims.getSubject();
            if (subject != null && !subject.equals(serverSession.getPrincipal().getNameId())) {
                rejectRequest(response, "JWT sujet invalide");
                return false;
            }
            HttpSession session = request.getSession(true);
            sessionHelper.storePrincipalInSession(session, serverSession.getPrincipal(), config.getSessionAttributeKey());
            session.setAttribute(serverSessionAttributeKey, serverSession.getSessionId());
            return true;
        } catch (SamlException ex) {
            rejectRequest(response, ex.getMessage());
            return false;
        }
    }

    public void attachJwtHeader(HttpServletRequest request, HttpServletResponse response) {
        if (!protectedRequestPredicate.test(request) || jwtService == null || response == null) {
            return;
        }
        HttpSession session = request.getSession(false);
        Optional<SamlPrincipal> principal = sessionHelper.retrievePrincipalFromSession(session, config.getSessionAttributeKey());
        if (principal.isEmpty()) {
            return;
        }
        SamlServerSession serverSession = resolveServerSession(session);
        if (serverSession == null) {
            return;
        }
        List<String> roles = extractRoles(serverSession.getPrincipal().getAttributes());
        String token = jwtService.issueToken(serverSession, jwtTtl, roles);
        response.setHeader(SamlJwtService.DEFAULT_HEADER_NAME, token);
    }

    private boolean isProtected(String requestUri, List<String> protectedPaths) {
        if (requestUri == null || protectedPaths == null) {
            return false;
        }
        return protectedPaths.stream().anyMatch(path -> matches(requestUri, path));
    }

    private boolean matches(String requestUri, String path) {
        if (path.endsWith("/*")) {
            String base = path.substring(0, path.length() - 2);
            return requestUri.startsWith(base);
        }
        return requestUri.equals(path);
    }

    private void renderPost(HttpServletResponse response, BindingMessage message) throws IOException {
        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType("text/html");
        String relayStateInput = message.getRelayState() == null ? "" :
                "<input type=\"hidden\" name=\"RelayState\" value=\"" + message.getRelayState() + "\" />";
        String html = """
                <html>
                  <body onload="document.forms[0].submit()">
                    <form method="post" action="%s">

                      <input type="hidden" name="SAMLRequest" value="%s" />

                      %s
                    </form>
                  </body>
                </html>
                """.formatted(message.getDestination(), message.getPayload(), relayStateInput);
        response.getWriter().write(html);
    }

    private String urlEncode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private boolean isServerSessionValid(HttpSession session, SamlPrincipal principal) {
        if (sessionRegistry == null || session == null) {
            return true;
        }
        SamlServerSession serverSession = resolveServerSession(session);
        if (serverSession == null) {
            return false;
        }
        String nameId = principal.getNameId();
        return nameId != null && nameId.equals(serverSession.getPrincipal().getNameId());
    }

    private SamlServerSession resolveServerSession(HttpSession session) {
        if (sessionRegistry == null || session == null) {
            return null;
        }
        Object attribute = session.getAttribute(serverSessionAttributeKey);
        if (!(attribute instanceof String sessionId)) {
            return null;
        }
        return sessionRegistry.getSession(sessionId);
    }

    private void createServerSession(SamlPrincipal principal, HttpSession session) {
        if (sessionRegistry == null || session == null) {
            return;
        }
        Instant now = Instant.now();
        Instant notOnOrAfter = extractInstant(principal.getAttributes().get(SamlAttributeKeys.NOT_ON_OR_AFTER));
        Instant maxExpiry = now.plus(sessionMaxTtl);
        Instant expiresAt = notOnOrAfter == null ? maxExpiry : minInstant(notOnOrAfter, maxExpiry);
        SamlServerSession serverSession = sessionRegistry.createSession(principal, expiresAt);
        session.setAttribute(serverSessionAttributeKey, serverSession.getSessionId());
    }

    private void invalidateServerSession(HttpSession session) {
        if (sessionRegistry == null || session == null) {
            return;
        }
        Object attribute = session.getAttribute(serverSessionAttributeKey);
        if (attribute instanceof String sessionId) {
            sessionRegistry.invalidate(sessionId);
        }
    }

    private Instant extractInstant(Object value) {
        if (value instanceof Instant) {
            return (Instant) value;
        }
        if (value instanceof String text) {
            try {
                return Instant.parse(text);
            } catch (Exception ignored) {
                return null;
            }
        }
        return null;
    }

    private Instant minInstant(Instant left, Instant right) {
        return left.isBefore(right) ? left : right;
    }

    private List<String> extractRoles(Map<String, Object> attributes) {
        if (attributes == null) {
            return List.of();
        }
        Object value = attributes.get("Role");
        if (value instanceof List<?> list) {
            List<String> roles = new ArrayList<>(list.size());
            for (Object item : list) {
                if (item != null) {
                    roles.add(item.toString());
                }
            }
            return roles;
        }
        if (value instanceof String text && !text.isBlank()) {
            return List.of(text);
        }
        return List.of();
    }

    private void rejectRequest(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("text/plain");
        response.getWriter().write(message == null ? "Unauthorized" : message);
    }
}
