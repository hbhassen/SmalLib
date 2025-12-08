package org.hmiso.saml.security;

import org.hmiso.saml.config.SecurityConfig;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.Objects;

/**
 * Service de signature/validation simplifié pour aligner l'API.
 */
public class SignatureService {
    private final SecurityConfig securityConfig;

    public SignatureService(SecurityConfig securityConfig) {
        this.securityConfig = Objects.requireNonNull(securityConfig, "securityConfig");
    }

    public String sign(String payload) {
        try {
            MessageDigest digest = MessageDigest.getInstance(securityConfig.getDigestAlgorithm());
            byte[] hash = digest.digest(payload.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (Exception e) {
            throw new IllegalStateException("Échec de signature résumée", e);
        }
    }

    public boolean validate(String payload, String signature) {
        return sign(payload).equals(signature);
    }
}
