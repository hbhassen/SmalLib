package org.hmiso.saml.integration;

import org.hmiso.saml.api.SamlPrincipal;

import javax.security.auth.Subject;
import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;
import java.util.function.Supplier;

/**
 * Utilitaires JaaS pour créer un Subject et un LoginContext (VII - JaasHelper).
 */
public class JaasHelper {
    private final SamlServerAuthModuleHelper samlServerAuthModuleHelper;
    private final Supplier<LoginContext> fallbackContextSupplier;

    public JaasHelper() {
        this(new SamlServerAuthModuleHelper(), null);
    }

    public JaasHelper(SamlServerAuthModuleHelper samlServerAuthModuleHelper, Supplier<LoginContext> fallbackContextSupplier) {
        this.samlServerAuthModuleHelper = samlServerAuthModuleHelper;
        this.fallbackContextSupplier = fallbackContextSupplier;
    }

    public Subject createJaasSubject(SamlPrincipal principal) {
        return samlServerAuthModuleHelper.createSubjectFromPrincipal(principal);
    }

    public LoginContext createLoginContext(SamlPrincipal principal, String loginContextName) throws LoginException {
        if (fallbackContextSupplier != null) {
            return fallbackContextSupplier.get();
        }
        return new LoginContext(loginContextName, createJaasSubject(principal));
    }
}
