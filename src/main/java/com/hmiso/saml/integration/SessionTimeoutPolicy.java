package com.hmiso.saml.integration;

import jakarta.servlet.http.HttpSession;

import java.util.Objects;
import java.util.function.Consumer;

/**
 * Politique de gestion de la durée de session (VII - SessionTimeoutPolicy).
 */
public final class SessionTimeoutPolicy {
    public enum SessionTimeoutAction {
        LOGOUT,
        WARN,
        REFRESH,
        NONE
    }

    private final boolean enforceSessionTimeout;
    private final SessionTimeoutAction sessionTimeoutAction;
    private final Consumer<HttpSession> onSessionExpiry;

    public SessionTimeoutPolicy() {
        this(true, SessionTimeoutAction.LOGOUT, HttpSession::invalidate);
    }

    public SessionTimeoutPolicy(boolean enforceSessionTimeout, SessionTimeoutAction sessionTimeoutAction, Consumer<HttpSession> onSessionExpiry) {
        this.enforceSessionTimeout = enforceSessionTimeout;
        this.sessionTimeoutAction = Objects.requireNonNull(sessionTimeoutAction, "sessionTimeoutAction");
        this.onSessionExpiry = Objects.requireNonNull(onSessionExpiry, "onSessionExpiry");
    }

    public boolean enforceSessionTimeout() {
        return enforceSessionTimeout;
    }

    public SessionTimeoutAction getSessionTimeoutAction() {
        return sessionTimeoutAction;
    }

    public void onSessionExpiry(HttpSession session) {
        onSessionExpiry.accept(session);
    }
}
