package com.hmiso.saml.saml;

import com.hmiso.saml.api.SamlException;
import com.hmiso.saml.config.SamlConfiguration;
import com.hmiso.saml.util.TimeUtils;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

/**
 * Validateur minimal appliquant la checklist securite de SPECIFICATION.md.
 */
public class SamlResponseValidator {
    private final SamlConfiguration configuration;

    public SamlResponseValidator(SamlConfiguration configuration) {
        this.configuration = Objects.requireNonNull(configuration, "configuration");
    }

    public void validate(SamlResponseValidationContext context) {
        // E8 / TC-SAML-02 : verifier audience, Destination, InResponseTo et fenetre temporelle.
        if (context.getAudience() == null
                || !configuration.getServiceProvider().getEntityId().equals(context.getAudience())) {
            throw new SamlException("AudienceRestriction invalide");
        }
        String expectedRecipient = configuration.getServiceProvider().getAssertionConsumerServiceUrl().toString();
        if (context.getDestination() == null || !expectedRecipient.equals(context.getDestination())) {
            throw new SamlException("Destination invalide");
        }
        if (context.getRecipient() != null && !expectedRecipient.equals(context.getRecipient())) {
            throw new SamlException("Recipient inattendu");
        }
        if (context.getInResponseTo() == null || context.getInResponseTo().isBlank()) {
            throw new SamlException("InResponseTo manquant");
        }
        if (context.getExpectedInResponseTo() != null
                && !context.getExpectedInResponseTo().equals(context.getInResponseTo())) {
            throw new SamlException("InResponseTo invalide");
        }
        if (!issuerMatches(configuration.getIdentityProvider().getEntityId(), context.getIssuer())) {
            throw new SamlException("Issuer inattendu");
        }
        if (context.getNotBefore() == null || context.getNotOnOrAfter() == null) {
            throw new SamlException("Conditions manquantes");
        }
        Duration skew = configuration.getSecurity().getClockSkewDuration();
        Instant now = Instant.now();
        if (!TimeUtils.isWithinClockSkew(now, context.getNotBefore(), context.getNotOnOrAfter(), skew)) {
            throw new SamlException("Horodatage en dehors de la fenetre autorisee");
        }

        boolean requireResponseSignature = configuration.getIdentityProvider().isWantMessagesSigned();
        boolean requireAssertionSignature = configuration.getIdentityProvider().isWantAssertionsSigned();
        if (requireResponseSignature && requireAssertionSignature) {
            if (!(context.isResponseSigned() || context.isAssertionSigned())) {
                throw new SamlException("Signature manquante");
            }
        } else {
            if (requireResponseSignature && !context.isResponseSigned()) {
                throw new SamlException("Signature response manquante");
            }
            if (requireAssertionSignature && !context.isAssertionSigned()) {
                throw new SamlException("Signature assertion manquante");
            }
        }
    }

    private boolean issuerMatches(String expected, String actual) {
        if (expected == null || expected.isBlank()) {
            return false;
        }
        if (actual == null || actual.isBlank()) {
            return false;
        }
        if (expected.equals(actual)) {
            return true;
        }
        if (!expected.contains("://") && actual.endsWith("/" + expected)) {
            return true;
        }
        return false;
    }
}
