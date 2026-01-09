package com.hmiso.saml.integration;

import com.hmiso.saml.DefaultSamlServiceProvider;
import com.hmiso.saml.TestConfigurations;
import com.hmiso.saml.api.SamlPrincipal;
import com.hmiso.saml.api.SamlServiceProvider;
import com.hmiso.saml.api.SamlServiceProviderFactory;
import com.hmiso.saml.config.BindingType;
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
        public SamlServiceProvider create(com.hmiso.saml.config.ConfigLoader loader) {
            return provider;
        }

        @Override
        public SamlServiceProvider create(com.hmiso.saml.config.SamlConfiguration configuration) {
            return provider;
        }
    }

    @Test
    void shouldAuditSuccessAndPropagatePrincipal_TC_INT_03() {
        var configuration = TestConfigurations.minimalConfig(BindingType.HTTP_REDIRECT);
        DefaultSamlServiceProvider provider = new DefaultSamlServiceProvider(configuration);
        AtomicBoolean audited = new AtomicBoolean(false);
        SamlAuditLogger auditLogger = new SamlAuditLogger() {
            @Override
            public void logAuthnRequestInitiated(com.hmiso.saml.binding.BindingMessage authnRequest) { }

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

        String samlResponse = buildResponse(configuration, "REQ-123", "user@example.com");
        SamlPrincipal principal = filter.onAcsResponse(provider, samlResponse, null);

        assertEquals("user@example.com", principal.getNameId());
        assertTrue(audited.get());
    }

    @Test
    void shouldRouteErrorsToHandler_TC_INT_01() {
        SamlServiceProvider failing = new SamlServiceProvider() {
            @Override
            public com.hmiso.saml.config.SamlConfiguration getConfiguration() {return TestConfigurations.minimalConfig(BindingType.HTTP_POST);}            @Override
            public com.hmiso.saml.binding.BindingMessage initiateAuthentication(String relayState) {return null;}
            @Override
            public SamlPrincipal processSamlResponse(String samlResponse, String relayState) {throw new IllegalStateException("boom");}
            @Override
            public com.hmiso.saml.binding.BindingMessage initiateLogout(String sessionIndex, String relayState) {return null;}
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

    private String buildResponse(com.hmiso.saml.config.SamlConfiguration configuration, String inResponseTo, String nameId) {
        String recipient = configuration.getServiceProvider().getAssertionConsumerServiceUrl().toString();
        String issuer = configuration.getIdentityProvider().getEntityId();
        String audience = configuration.getServiceProvider().getEntityId();
        java.time.Instant now = java.time.Instant.now();
        java.time.Instant notOnOrAfter = now.plusSeconds(30);
        return """
                <samlp:Response xmlns:samlp="urn:oasis:names:tc:SAML:2.0:protocol"
                                xmlns:saml="urn:oasis:names:tc:SAML:2.0:assertion"
                                xmlns:ds="http://www.w3.org/2000/09/xmldsig#"
                                ID="R1" InResponseTo="%s" Destination="%s">
                  <saml:Issuer>%s</saml:Issuer>
                  <ds:Signature>sig</ds:Signature>
                  <saml:Assertion ID="A1">
                    <saml:Issuer>%s</saml:Issuer>
                    <ds:Signature>sig</ds:Signature>
                    <saml:Subject>
                      <saml:NameID Format="urn:oasis:names:tc:SAML:1.1:nameid-format:emailAddress">%s</saml:NameID>
                      <saml:SubjectConfirmation>
                        <saml:SubjectConfirmationData Recipient="%s" NotOnOrAfter="%s" />
                      </saml:SubjectConfirmation>
                    </saml:Subject>
                    <saml:Conditions NotBefore="%s" NotOnOrAfter="%s">
                      <saml:AudienceRestriction>
                        <saml:Audience>%s</saml:Audience>
                      </saml:AudienceRestriction>
                    </saml:Conditions>
                    <saml:AuthnStatement SessionIndex="sess-1" />
                  </saml:Assertion>
                </samlp:Response>
                """.formatted(inResponseTo, recipient, issuer, issuer, nameId, recipient, notOnOrAfter,
                now, notOnOrAfter, audience);
    }
}
