package com.hmiso.saml.saml;

import com.hmiso.saml.api.SamlException;
import com.hmiso.saml.config.SamlConfiguration;
import com.hmiso.saml.util.TimeUtils;

import java.time.Instant;
import java.util.Objects;

/**
 * Valide un LogoutResponse selon les exigences de SLO.
 */
public class LogoutResponseValidator {
    private final SamlConfiguration configuration;

    public LogoutResponseValidator(SamlConfiguration configuration) {
        this.configuration = Objects.requireNonNull(configuration, "configuration");
    }

    public void validate(String inResponseTo, Instant notBefore, Instant notOnOrAfter) {
        if (inResponseTo == null || inResponseTo.isBlank()) {
            throw new SamlException("InResponseTo manquant dans LogoutResponse");
        }
        Instant now = Instant.now();
        if (!TimeUtils.isWithinClockSkew(now, notBefore, notOnOrAfter, configuration.getSecurity().getClockSkewDuration())) {
            throw new SamlException("Horodatage de LogoutResponse invalide");
        }
    }
}
