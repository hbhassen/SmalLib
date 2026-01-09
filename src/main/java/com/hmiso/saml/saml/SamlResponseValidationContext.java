package com.hmiso.saml.saml;

import java.time.Instant;
/**
 * Donnees extraites de la SAMLResponse necessaires a la validation.
 */
public final class SamlResponseValidationContext {
    private final String audience;
    private final String recipient;
    private final String inResponseTo;
    private final String expectedInResponseTo;
    private final Instant notBefore;
    private final Instant notOnOrAfter;
    private final String issuer;
    private final String destination;
    private final boolean responseSigned;
    private final boolean assertionSigned;

    public SamlResponseValidationContext(String audience,
                                         String recipient,
                                         String inResponseTo,
                                         String expectedInResponseTo,
                                         Instant notBefore,
                                         Instant notOnOrAfter,
                                         String issuer,
                                         String destination,
                                         boolean responseSigned,
                                         boolean assertionSigned) {
        this.audience = audience;
        this.recipient = recipient;
        this.inResponseTo = inResponseTo;
        this.expectedInResponseTo = expectedInResponseTo;
        this.notBefore = notBefore;
        this.notOnOrAfter = notOnOrAfter;
        this.issuer = issuer;
        this.destination = destination;
        this.responseSigned = responseSigned;
        this.assertionSigned = assertionSigned;
    }

    public String getAudience() {
        return audience;
    }

    public String getRecipient() {
        return recipient;
    }

    public String getInResponseTo() {
        return inResponseTo;
    }

    public String getExpectedInResponseTo() {
        return expectedInResponseTo;
    }

    public Instant getNotBefore() {
        return notBefore;
    }

    public Instant getNotOnOrAfter() {
        return notOnOrAfter;
    }

    public String getIssuer() {
        return issuer;
    }

    public String getDestination() {
        return destination;
    }

    public boolean isResponseSigned() {
        return responseSigned;
    }

    public boolean isAssertionSigned() {
        return assertionSigned;
    }
}
