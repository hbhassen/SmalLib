package org.hmiso.saml.security;

import org.hmiso.saml.config.KeystoreConfig;
import org.hmiso.saml.config.SecurityConfig;

import java.nio.file.Files;
import java.util.Objects;

/**
 * Résout les credentials à partir des keystores/truststores fournis.
 */
public class CredentialResolver {
    private final SecurityConfig securityConfig;

    public CredentialResolver(SecurityConfig securityConfig) {
        this.securityConfig = Objects.requireNonNull(securityConfig, "securityConfig");
    }

    public void validateKeystores() {
        validateConfig(securityConfig.getKeystore());
        validateConfig(securityConfig.getTruststore());
    }

    private void validateConfig(KeystoreConfig config) {
        if (config == null) {
            return;
        }
        if (!Files.exists(config.getPath())) {
            throw new IllegalStateException("Keystore introuvable : " + config.getPath());
        }
        if (config.getPassword() == null || config.getPassword().isBlank()) {
            throw new IllegalStateException("Mot de passe keystore manquant pour " + config.getPath());
        }
    }
}
