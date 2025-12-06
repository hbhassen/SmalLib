package com.hmiso.saml.config;

import java.nio.file.Path;
import java.util.Objects;

/**
 * Paramètres de keystore ou truststore utilisés par les services de sécurité.
 */
public final class KeystoreConfig {
    private final Path path;
    private final String password;
    private final String keyAlias;
    private final String keyPassword;
    private final String type;

    public KeystoreConfig(Path path, String password, String keyAlias, String keyPassword, String type) {
        this.path = Objects.requireNonNull(path, "path");
        this.password = Objects.requireNonNull(password, "password");
        this.keyAlias = keyAlias;
        this.keyPassword = keyPassword;
        this.type = Objects.requireNonNull(type, "type");
    }

    public Path getPath() {
        return path;
    }

    public String getPassword() {
        return password;
    }

    public String getKeyAlias() {
        return keyAlias;
    }

    public String getKeyPassword() {
        return keyPassword;
    }

    public String getType() {
        return type;
    }
}
