package org.hmiso.saml.saml;

import org.hmiso.saml.config.BindingType;
import org.hmiso.saml.config.SamlConfiguration;

import java.time.Instant;
import java.util.Objects;

/**
 * Construction minimale d'une AuthnRequest SAML valide (namespaces, Issuer, NameIDPolicy).
 */
public class AuthnRequestBuilder {
    private final SamlConfiguration configuration;

    public AuthnRequestBuilder(SamlConfiguration configuration) {
        this.configuration = Objects.requireNonNull(configuration, "configuration");
    }

    public String build(String requestId, Instant issueInstant) {
        // E7 / TC-SAML-01 : générer une AuthnRequest SAML 2.0 alignée sur l'ACS et le SSO configurés.
        String protocolBinding = bindingUrn(configuration.getServiceProvider().getAuthnRequestBinding());
        return """
                <samlp:AuthnRequest xmlns:samlp="urn:oasis:names:tc:SAML:2.0:protocol"
                                    xmlns:saml="urn:oasis:names:tc:SAML:2.0:assertion"
                                    ID="%s"
                                    Version="2.0"
                                    IssueInstant="%s"
                                    Destination="%s"
                                    ProtocolBinding="%s"
                                    AssertionConsumerServiceURL="%s">
                  <saml:Issuer>%s</saml:Issuer>
                  <samlp:NameIDPolicy
                      AllowCreate="true"
                      Format="%s" />
                </samlp:AuthnRequest>
                """.formatted(
                requestId,
                issueInstant,
                configuration.getIdentityProvider().getSingleSignOnServiceUrl(),
                protocolBinding,
                configuration.getServiceProvider().getAssertionConsumerServiceUrl(),
                configuration.getServiceProvider().getEntityId(),
                configuration.getServiceProvider().getNameIdFormat()
        );
    }

    private String bindingUrn(BindingType bindingType) {
        if (bindingType == BindingType.HTTP_REDIRECT) {
            return "urn:oasis:names:tc:SAML:2.0:bindings:HTTP-Redirect";
        }
        return "urn:oasis:names:tc:SAML:2.0:bindings:HTTP-POST";
    }
}
