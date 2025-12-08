package org.hmiso.saml.integration;

import org.hmiso.saml.DefaultSamlServiceProvider;
import org.hmiso.saml.TestConfigurations;
import org.hmiso.saml.api.SamlPrincipal;
import org.hmiso.saml.api.SamlServiceProvider;
import org.hmiso.saml.api.SamlServiceProviderFactory;
import org.hmiso.saml.config.BindingType;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SamlFilterTest {

    private static class StubFactory implements SamlServiceProviderFactory {
        private final SamlServiceProvider provider;

        StubFactory(SamlServiceProvider provider) {
            this.provider = provider;
        }

        @Override
        public SamlServiceProvider create(org.hmiso.saml.config.ConfigLoader loader) {
            return provider;
        }

        @Override
        public SamlServiceProvider create(org.hmiso.saml.config.SamlConfiguration configuration) {
            return provider;
        }
    }

    @Test
    void shouldAuditSuccessAndPropagatePrincipal_TC_INT_03() {
        DefaultSamlServiceProvider provider = new DefaultSamlServiceProvider(TestConfigurations.minimalConfig(BindingType.HTTP_REDIRECT));
        AtomicBoolean audited = new AtomicBoolean(false);
        SamlAuditLogger auditLogger = new SamlAuditLogger() {
            @Override
            public void logAuthnRequestInitiated(org.hmiso.saml.binding.BindingMessage authnRequest) { }

            @Override
            public void logAuthenticationSuccess(SamlPrincipal principal) {
                audited.set(true);
            }

            @Override
            public void logAuthenticationFailure(Exception error) { }

            @Override
            public void logLogoutInitiated(SamlPrincipal principal) { }

            @Override
            public void logLogoutSuccess(String sessionIndex) { }
        };
        SamlFilter filter = new SamlFilter(new StubFactory(provider), auditLogger, null);

        SamlPrincipal principal = filter.onAcsResponse(provider, "user@example.com", null);

        assertEquals("user@example.com", principal.getNameId());
        assertTrue(audited.get());
    }

    @Test
    void shouldRouteErrorsToHandler_TC_INT_01() {
        SamlServiceProvider failing = new SamlServiceProvider() {
            @Override
            public org.hmiso.saml.config.SamlConfiguration getConfiguration() {return TestConfigurations.minimalConfig(BindingType.HTTP_POST);}            @Override
            public org.hmiso.saml.binding.BindingMessage initiateAuthentication(String relayState) {return null;}
            @Override
            public SamlPrincipal processSamlResponse(String samlResponse, String relayState) {throw new IllegalStateException("boom");}
            @Override
            public org.hmiso.saml.binding.BindingMessage initiateLogout(String sessionIndex, String relayState) {return null;}
            @Override
            public void processLogoutResponse(String logoutResponse, String inResponseTo) { }
        };
        AtomicBoolean handled = new AtomicBoolean(false);
        SamlFilter filter = new SamlFilter(new StubFactory(failing), null, new SamlErrorHandler() {
            @Override
            public String handleValidationError(Throwable error) {
                handled.set(true);
                return "/error";
            }

            @Override
            public String handleSecurityError(Throwable error) {return "/error";}

            @Override
            public String handleBindingError(Throwable error) {return "/error";}
        });

        try {
            filter.onAcsResponse(failing, "", null);
        } catch (Exception ignored) {
            // expected
        }

        assertTrue(handled.get());
    }
}
