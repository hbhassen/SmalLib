package org.hmiso.saml.integration;

import org.hmiso.saml.api.SamlPrincipal;
import org.hmiso.saml.binding.BindingMessage;
import org.hmiso.saml.binding.RelayStateStore;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
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
            BindingMessage logout = config.getSamlServiceProvider().initiateLogout(p.getSessionIndex(), request.getRequestURI());
            if (auditLogger != null) {
                auditLogger.logLogoutSuccess(p.getSessionIndex());
            }
            if (response != null) {
                try {
                    response.sendRedirect(logout.getDestination().toString());
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
}
