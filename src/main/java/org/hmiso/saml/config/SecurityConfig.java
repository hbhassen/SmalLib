package org.hmiso.saml.config;

import java.time.Duration;
import java.util.Objects;

/**
 * Paramétrage de la sécurité SAML (signatures, horodatages, keystores).
 */
public final class SecurityConfig {
    private final Duration clockSkewDuration;
    private final String signatureAlgorithm;
    private final String digestAlgorithm;
    private final KeystoreConfig keystore;
    private final KeystoreConfig truststore;
    private final String encryptionAlgorithm;
    private final boolean forceHttpsRedirect;
    private final boolean enableDetailedLogging;

    public SecurityConfig(Duration clockSkewDuration,
                          String signatureAlgorithm,
                          String digestAlgorithm,
                          KeystoreConfig keystore,
                          KeystoreConfig truststore,
                          String encryptionAlgorithm,
                          boolean forceHttpsRedirect,
                          boolean enableDetailedLogging) {
        this.clockSkewDuration = Objects.requireNonNull(clockSkewDuration, "clockSkewDuration");
        this.signatureAlgorithm = Objects.requireNonNull(signatureAlgorithm, "signatureAlgorithm");
        this.digestAlgorithm = Objects.requireNonNull(digestAlgorithm, "digestAlgorithm");
        this.keystore = keystore;
        this.truststore = truststore;
        this.encryptionAlgorithm = encryptionAlgorithm;
        this.forceHttpsRedirect = forceHttpsRedirect;
        this.enableDetailedLogging = enableDetailedLogging;
    }

    public Duration getClockSkewDuration() {
        return clockSkewDuration;
    }

    public String getSignatureAlgorithm() {
        return signatureAlgorithm;
    }

    public String getDigestAlgorithm() {
        return digestAlgorithm;
    }

    public KeystoreConfig getKeystore() {
        return keystore;
    }

    public KeystoreConfig getTruststore() {
        return truststore;
    }

    public String getEncryptionAlgorithm() {
        return encryptionAlgorithm;
    }

    public boolean isForceHttpsRedirect() {
        return forceHttpsRedirect;
    }

    public boolean isEnableDetailedLogging() {
        return enableDetailedLogging;
    }
}
