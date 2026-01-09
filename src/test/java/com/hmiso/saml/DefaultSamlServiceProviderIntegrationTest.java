package com.hmiso.saml;

import com.hmiso.saml.api.SamlPrincipal;
import com.hmiso.saml.binding.BindingMessage;
import com.hmiso.saml.config.BindingType;
import com.hmiso.saml.config.SamlConfiguration;
import com.hmiso.saml.util.CompressionUtils;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class DefaultSamlServiceProviderIntegrationTest {

    @Test
    void shouldInitiateRedirectFlowAndValidateResponse_TC_E2E_AUTHN_01() {
        SamlConfiguration configuration = TestConfigurations.minimalConfig(BindingType.HTTP_REDIRECT);
        DefaultSamlServiceProvider provider = new DefaultSamlServiceProvider(configuration);

        BindingMessage message = provider.initiateAuthentication(null);
        String xml = CompressionUtils.inflateFromBase64(message.getPayload());
        assertEquals(BindingType.HTTP_REDIRECT, message.getBindingType());
        assertNotNull(xml);

        String requestId = extractRequestId(xml);
        String samlResponse = buildResponse(configuration, requestId, "user@example.com");
        SamlPrincipal principal = provider.processSamlResponse(samlResponse, message.getRelayState());
        assertEquals("user@example.com", principal.getNameId());
    }

    @Test
    void shouldInitiatePostLogout_TC_E2E_LOGOUT_01() {
        SamlConfiguration configuration = TestConfigurations.minimalConfig(BindingType.HTTP_POST);
        DefaultSamlServiceProvider provider = new DefaultSamlServiceProvider(configuration);

        BindingMessage message = provider.initiateLogout("session-1", "relay");
        String xml = new String(Base64.getDecoder().decode(message.getPayload()));
        assertEquals(BindingType.HTTP_POST, message.getBindingType());
        assertNotNull(xml);

        provider.processLogoutResponse("<LogoutResponse/>", "LR-123");
    }

    private String extractRequestId(String xml) {
        int idIndex = xml.indexOf("ID=\"");
        if (idIndex < 0) {
            return "REQ-UNKNOWN";
        }
        int start = idIndex + 4;
        int end = xml.indexOf("\"", start);
        if (end < 0) {
            return "REQ-UNKNOWN";
        }
        return xml.substring(start, end);
    }

    private String buildResponse(SamlConfiguration configuration, String inResponseTo, String nameId) {
        String recipient = configuration.getServiceProvider().getAssertionConsumerServiceUrl().toString();
        String issuer = configuration.getIdentityProvider().getEntityId();
        String audience = configuration.getServiceProvider().getEntityId();
        Instant now = Instant.now();
        Instant notOnOrAfter = now.plusSeconds(30);
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
