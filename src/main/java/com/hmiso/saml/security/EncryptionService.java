package com.hmiso.saml.security;

import com.hmiso.saml.config.SecurityConfig;

import java.util.Objects;

/**
 * Service de chiffrement/déchiffrement optionnel.
 */
public class EncryptionService {
    private final SecurityConfig securityConfig;

    public EncryptionService(SecurityConfig securityConfig) {
        this.securityConfig = Objects.requireNonNull(securityConfig, "securityConfig");
    }

    public String encrypt(String assertion) {
        // Placeholder pour intégration future OpenSAML
        return assertion;
    }

    public String decrypt(String encryptedAssertion) {
        // Placeholder pour intégration future OpenSAML
        return encryptedAssertion;
    }
}
