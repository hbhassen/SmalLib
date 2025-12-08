package org.hmiso.saml.saml;

import org.hmiso.saml.api.SamlException;
import org.hmiso.saml.config.SamlConfiguration;
import org.hmiso.saml.util.TimeUtils;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

/**
 * Validateur minimal appliquant la checklist sécurité de SPECIFICATION.md.
 */
public class SamlResponseValidator {
    private final SamlConfiguration configuration;

    public SamlResponseValidator(SamlConfiguration configuration) {
        this.configuration = Objects.requireNonNull(configuration, "configuration");
    }

    public void validate(String audience, String recipient, String inResponseTo, Instant notBefore, Instant notOnOrAfter) {
        // E8 / TC-SAML-02 : vérifier audience, Recipient, InResponseTo et fenêtre temporelle avec clockSkew.
        if (!configuration.getServiceProvider().getEntityId().equals(audience)) {
            throw new SamlException("AudienceRestriction invalide");
        }
        if (!configuration.getServiceProvider().getAssertionConsumerServiceUrl().toString().equals(recipient)) {
            throw new SamlException("Recipient inattendu");
        }
        if (inResponseTo == null || inResponseTo.isBlank()) {
            throw new SamlException("InResponseTo manquant");
        }
        Duration skew = configuration.getSecurity().getClockSkewDuration();
        Instant now = Instant.now();
        if (!TimeUtils.isWithinClockSkew(now, notBefore, notOnOrAfter, skew)) {
            throw new SamlException("Horodatage en dehors de la fenêtre autorisée");
        }
    }
}
