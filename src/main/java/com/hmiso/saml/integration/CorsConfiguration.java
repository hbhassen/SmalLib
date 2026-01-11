package com.hmiso.saml.integration;

import java.util.List;
import java.util.Objects;

public final class CorsConfiguration {
    public static final List<String> DEFAULT_ALLOWED_METHODS = List.of("GET", "POST", "OPTIONS");
    public static final List<String> DEFAULT_ALLOWED_HEADERS = List.of("Authorization", "Content-Type", "X-Requested-With");
    public static final List<String> DEFAULT_EXPOSE_HEADERS = List.of("X-Auth-Token");

    private final boolean enabled;
    private final boolean allowCredentials;
    private final List<String> allowedOrigins;
    private final List<String> allowedMethods;
    private final List<String> allowedHeaders;
    private final List<String> exposeHeaders;

    public CorsConfiguration(boolean enabled,
                             boolean allowCredentials,
                             List<String> allowedOrigins,
                             List<String> allowedMethods,
                             List<String> allowedHeaders,
                             List<String> exposeHeaders) {
        this.enabled = enabled;
        this.allowCredentials = allowCredentials;
        this.allowedOrigins = List.copyOf(Objects.requireNonNull(allowedOrigins, "allowedOrigins"));
        this.allowedMethods = List.copyOf(Objects.requireNonNull(allowedMethods, "allowedMethods"));
        this.allowedHeaders = List.copyOf(Objects.requireNonNull(allowedHeaders, "allowedHeaders"));
        this.exposeHeaders = List.copyOf(Objects.requireNonNull(exposeHeaders, "exposeHeaders"));
    }

    public boolean isEnabled() {
        return enabled;
    }

    public boolean isAllowCredentials() {
        return allowCredentials;
    }

    public List<String> getAllowedOrigins() {
        return allowedOrigins;
    }

    public List<String> getAllowedMethods() {
        return allowedMethods;
    }

    public List<String> getAllowedHeaders() {
        return allowedHeaders;
    }

    public List<String> getExposeHeaders() {
        return exposeHeaders;
    }
}
