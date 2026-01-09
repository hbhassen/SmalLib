package com.hmiso.saml.integration;

import com.hmiso.saml.api.SamlPrincipal;
import com.hmiso.saml.binding.BindingMessage;
import com.hmiso.saml.binding.RelayStateStore;
import com.hmiso.saml.config.BindingType;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * Logique centrale du filtre Servlet décrit dans le module VII.
 */
public class SamlAuthenticationFilterHelper {
    private final SamlAuthenticationFilterConfig config;
    private final SamlSessionHelper sessionHelper;
    private final SamlAuditLogger auditLogger;
    private final SamlErrorHandler errorHandler;
    private final Predicate<HttpServletRequest> protectedRequestPredicate;

    public SamlAuthenticationFilterHelper(SamlAuthenticationFilterConfig config,
                                          SamlSessionHelper sessionHelper,
                                          SamlAuditLogger auditLogger,
                                          SamlErrorHandler errorHandler) {
        this.config = config;
        this.sessionHelper = sessionHelper;
        this.auditLogger = auditLogger;
        this.errorHandler = errorHandler;
        this.protectedRequestPredicate = req -> isProtected(req.getRequestURI(), config.getProtectedPaths());
    }

    public Optional<BindingMessage> shouldRedirectToIdP(HttpServletRequest request, HttpServletResponse response) {
        if (!protectedRequestPredicate.test(request)) {
            return Optional.empty();
        }
        Optional<SamlPrincipal> existing = sessionHelper.retrievePrincipalFromSession(request.getSession(false),
                config.getSessionAttributeKey());
        if (existing.isPresent()) {
            return Optional.empty();
        }
        String originalUri = request.getRequestURI();
        // Enregistre un relay state serveur (si disponible) pour restaurer l'URL initiale après ACS
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
            sessionHelper.storePrincipalInSession(request.getSession(true), principal, config.getSessionAttributeKey());
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
        });
        sessionHelper.invalidateSession(session);
    }

    public Optional<SamlPrincipal> extractPrincipalFromSession(HttpSession session) {
        return sessionHelper.retrievePrincipalFromSession(session, config.getSessionAttributeKey());
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
                  <body onload=\"document.forms[0].submit()\">
                    <form method=\"post\" action=\"%s\">\n
                      <input type=\"hidden\" name=\"SAMLRequest\" value=\"%s\" />\n
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
}
